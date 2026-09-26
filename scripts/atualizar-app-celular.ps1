param(
    [string]$PhoneSerial = "ZF52554B2L",
    [switch]$SkipTests,
    [switch]$SafeBuild,
    [switch]$TestRemoteAccess
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $ProjectRoot

$Sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$Adb = Join-Path $Sdk "platform-tools\adb.exe"
$Gradle = Join-Path $ProjectRoot "gradlew.bat"
$Apk = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"
$PackageName = "br.com.leitorcuponsfinancas"
$Branch = "feat/android-ms-mvp"

function Get-PhysicalDevices {
    & $Adb start-server | Out-Null

    return @(
        & $Adb devices |
            Where-Object {
                $_ -match "^[^\s]+\s+device$" -and
                $_ -notmatch "^emulator-"
            } |
            ForEach-Object {
                ($_ -split "\s+")[0]
            }
    )
}

function Get-UnauthorizedPhysicalDevices {
    return @(
        & $Adb devices |
            Where-Object {
                $_ -match "^[^\s]+\s+unauthorized$" -and
                $_ -notmatch "^emulator-"
            } |
            ForEach-Object {
                ($_ -split "\s+")[0]
            }
    )
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " LEITOR CUPONS FINANCAS - ATUALIZAR CELULAR" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Modo de testes: aparelho Android fisico" -ForegroundColor DarkCyan
Write-Host "O Emulator NAO sera iniciado nem utilizado por este script." -ForegroundColor DarkCyan

foreach ($Required in @($Adb, $Gradle)) {
    if (-not (Test-Path $Required)) {
        throw "Arquivo necessario nao encontrado: $Required"
    }
}

Write-Host ""
Write-Host "[1/7] Conferindo repositorio..." -ForegroundColor Yellow

$Dirty = git status --porcelain
if ($LASTEXITCODE -ne 0) {
    throw "Nao foi possivel consultar o estado do Git."
}

if ($Dirty) {
    Write-Host ""
    git status --short
    throw "O repositorio possui alteracoes locais. A atualizacao foi interrompida para preservar seus arquivos."
}

git fetch --prune origin
if ($LASTEXITCODE -ne 0) {
    throw "Falha no git fetch."
}

git switch $Branch
if ($LASTEXITCODE -ne 0) {
    throw "Nao foi possivel mudar para a branch $Branch."
}

git pull --ff-only origin $Branch
if ($LASTEXITCODE -ne 0) {
    throw "Falha ao atualizar a branch $Branch."
}

$Commit = (git rev-parse --short HEAD).Trim()
$AppCommit = (git log -1 --format="%h" -- app).Trim()
$AppCommitDescription = (git log -1 --oneline -- app).Trim()

Write-Host "[OK] Branch atualizada: $Branch @ $Commit" -ForegroundColor Green
Write-Host "Ultimo commit que alterou app/: $AppCommitDescription" -ForegroundColor Cyan

Write-Host ""
Write-Host "[2/7] Conferindo celular..." -ForegroundColor Yellow

$Unauthorized = Get-UnauthorizedPhysicalDevices
if ($Unauthorized.Count -gt 0) {
    throw "Celular conectado, mas nao autorizado. Desbloqueie o aparelho e confirme 'Permitir depuracao USB'."
}

$PhysicalDevices = Get-PhysicalDevices

if ($PhoneSerial) {
    if ($PhysicalDevices -notcontains $PhoneSerial) {
        Write-Host ""
        & $Adb devices -l
        throw "O celular '$PhoneSerial' nao esta conectado/autorizado no ADB."
    }
    $Target = $PhoneSerial
} else {
    if ($PhysicalDevices.Count -ne 1) {
        & $Adb devices -l
        throw "Conecte exatamente um celular Android ou informe -PhoneSerial."
    }
    $Target = $PhysicalDevices[0]
}

$Model = (& $Adb -s $Target shell getprop ro.product.model).Trim()
Write-Host "[OK] Celular autorizado: $Model ($Target)" -ForegroundColor Green

Write-Host ""
Write-Host "[3/7] Executando testes unitarios..." -ForegroundColor Yellow

if (-not $SkipTests) {
    if ($SafeBuild) {
        & $Gradle testDebugUnitTest --no-daemon --max-workers=1
    } else {
        & $Gradle testDebugUnitTest --max-workers=2
    }

    if ($LASTEXITCODE -ne 0) {
        throw "Os testes falharam. Nada sera instalado no celular."
    }

    Write-Host "[OK] Testes aprovados." -ForegroundColor Green
} else {
    Write-Host "[ATENCAO] Testes ignorados por parametro -SkipTests." -ForegroundColor DarkYellow
}

Write-Host ""
Write-Host "[4/7] Gerando APK..." -ForegroundColor Yellow

$BuildArguments = @("assembleDebug")

if ($SafeBuild) {
    $BuildArguments += "--no-daemon"
    $BuildArguments += "--max-workers=1"
} else {
    $BuildArguments += "--max-workers=2"
}

if ($TestRemoteAccess) {
    $BuildArguments += "-PLCF_REMOTE_ACCESS_REQUIRED=true"
    Write-Host "[MODO TESTE] Login/autorizacao remota habilitados neste APK debug." -ForegroundColor Cyan
} else {
    Write-Host "[MODO DESENVOLVIMENTO] APK debug sem bloqueio por login remoto." -ForegroundColor DarkCyan
}

& $Gradle @BuildArguments

if ($LASTEXITCODE -ne 0) {
    throw "Falha ao gerar o APK. Nada sera instalado no celular."
}

if (-not (Test-Path $Apk)) {
    throw "APK nao encontrado: $Apk"
}

$ApkHash = (Get-FileHash -Path $Apk -Algorithm SHA256).Hash

Write-Host "[OK] APK gerado." -ForegroundColor Green
Write-Host "APK: $Apk"
Write-Host "SHA256: $ApkHash"

Write-Host ""
Write-Host "[5/7] Instalando no celular sem apagar dados..." -ForegroundColor Yellow

$InstallOutput = @(& $Adb -s $Target install -r $Apk 2>&1)
$InstallExitCode = $LASTEXITCODE
$InstallText = $InstallOutput -join [Environment]::NewLine

$InstallOutput | ForEach-Object { Write-Host $_ }

if ($InstallExitCode -ne 0 -or $InstallText -notmatch "(?m)^Success\s*$") {
    throw "A instalacao nao foi confirmada pelo ADB com 'Success'."
}

Write-Host "[OK] Instalacao confirmada pelo ADB." -ForegroundColor Green

Write-Host ""
Write-Host "[6/7] Abrindo aplicativo..." -ForegroundColor Yellow

& $Adb -s $Target shell am force-stop $PackageName | Out-Null
Start-Sleep -Seconds 1
& $Adb -s $Target shell am start -n "$PackageName/.MainActivity"

if ($LASTEXITCODE -ne 0) {
    throw "O APK foi instalado, mas o aplicativo nao abriu automaticamente."
}

Write-Host "[OK] Aplicativo aberto no celular." -ForegroundColor Green

Write-Host ""
Write-Host "[7/7] Resumo..." -ForegroundColor Yellow
Write-Host "Repositorio: $Commit"
Write-Host "Codigo Android: $AppCommitDescription"
Write-Host "Celular: $Model"
Write-Host "Serial: $Target"
Write-Host "SHA256 APK: $ApkHash"

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host " CELULAR ATUALIZADO COM SUCESSO" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "Dados locais preservados: SIM"
Write-Host "Metodo: adb install -r"
Write-Host "Emulator utilizado: NAO"
Write-Host "Rotacao/configuracoes Android alteradas: NAO"
Write-Host "pm clear / uninstall executados: NAO"
Write-Host "Controle remoto exigido neste debug: $($TestRemoteAccess.IsPresent)"
