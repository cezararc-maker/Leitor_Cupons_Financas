# USO OPCIONAL / SOMENTE QUANDO O EMULATOR FOR SOLICITADO EXPLICITAMENTE.
# O fluxo padrao do projeto e scripts/atualizar-app-celular.ps1.
# Este arquivo e mantido apenas para testes futuros que exijam Emulator.

param(
    [string]$PhoneSerial,
    [switch]$SkipBuild,
    [switch]$SafeBuild
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $ProjectRoot

$Sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$Adb = Join-Path $Sdk "platform-tools\adb.exe"
$Gradle = Join-Path $ProjectRoot "gradlew.bat"
$Apk = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"
$PackageName = "br.com.leitorcuponsfinancas"
$EmulatorScript = Join-Path $ProjectRoot "scripts\executar-app-emulador.ps1"
$Branch = "feat/android-ms-mvp"

function Get-AdbDevices {
    & $Adb start-server | Out-Null
    return @(& $Adb devices)
}

function Get-PhysicalDevices {
    param([string[]]$Lines)

    return @(
        $Lines |
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
    param([string[]]$Lines)

    return @(
        $Lines |
            Where-Object {
                $_ -match "^[^\s]+\s+unauthorized$" -and
                $_ -notmatch "^emulator-"
            } |
            ForEach-Object {
                ($_ -split "\s+")[0]
            }
    )
}

function Get-EmulatorSerial {
    param([string[]]$Lines)

    $Line = $Lines | Where-Object {
        $_ -match "^(emulator-\d+)\s+device$"
    } | Select-Object -First 1

    if ($Line -and $Line -match "^(emulator-\d+)\s+device$") {
        return $Matches[1]
    }

    return $null
}

function Install-And-Open {
    param(
        [string]$Serial,
        [string]$Label
    )

    Write-Host ""
    Write-Host "[INSTALANDO] $Label - $Serial" -ForegroundColor Yellow

    & $Adb -s $Serial install -r $Apk
    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao instalar o APK em $Label ($Serial)."
    }

    & $Adb -s $Serial shell am force-stop $PackageName | Out-Null
    & $Adb -s $Serial shell am start -n "$PackageName/.MainActivity" | Out-Null

    if ($LASTEXITCODE -ne 0) {
        throw "APK instalado em $Label, mas o app nao abriu automaticamente."
    }

    Write-Host "[OK] $Label atualizado com o mesmo APK." -ForegroundColor Green
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " LEITOR CUPONS FINANCAS - ATUALIZAR EMULADOR + CELULAR" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

foreach ($Required in @($Adb, $Gradle, $EmulatorScript)) {
    if (-not (Test-Path $Required)) {
        throw "Arquivo necessario nao encontrado: $Required"
    }
}

Write-Host ""
Write-Host "[1/6] Conferindo repositorio..." -ForegroundColor Yellow

$Dirty = git status --porcelain
if ($LASTEXITCODE -ne 0) {
    throw "Nao foi possivel consultar o estado do Git."
}

if ($Dirty) {
    throw @"
O repositorio possui alteracoes locais nao commitadas.
Para evitar sobrescrever trabalho local, a atualizacao foi interrompida.

Execute:
git status

Resolva as alteracoes locais antes de tentar novamente.
"@
}

git fetch --prune
if ($LASTEXITCODE -ne 0) {
    throw "Falha no git fetch."
}

git switch $Branch
if ($LASTEXITCODE -ne 0) {
    throw "Nao foi possivel mudar para o branch $Branch."
}

git pull --ff-only origin $Branch
if ($LASTEXITCODE -ne 0) {
    throw "Falha ao atualizar o branch $Branch."
}

$Commit = (git rev-parse --short HEAD).Trim()
Write-Host "[OK] Branch atualizado: $Branch @ $Commit" -ForegroundColor Green

Write-Host ""
Write-Host "[2/6] Conferindo celular antes do build..." -ForegroundColor Yellow

$DeviceLines = Get-AdbDevices
$Unauthorized = Get-UnauthorizedPhysicalDevices -Lines $DeviceLines

if ($Unauthorized.Count -gt 0) {
    throw "Celular conectado, mas nao autorizado. Desbloqueie o aparelho e confirme 'Permitir depuracao USB'."
}

$PhysicalDevices = Get-PhysicalDevices -Lines $DeviceLines

if ($PhoneSerial) {
    if ($PhysicalDevices -notcontains $PhoneSerial) {
        throw "O celular '$PhoneSerial' nao esta conectado/autorizado no ADB."
    }
    $PhysicalDevices = @($PhoneSerial)
}

if ($PhysicalDevices.Count -eq 0) {
    throw @"
Nenhum celular Android conectado e autorizado.

Para manter Emulator e celular sempre na mesma versao, nada foi atualizado.

Conecte o celular por USB, ative a Depuracao USB e confirme a autorizacao.
Depois execute este script novamente.
"@
}

Write-Host "[OK] Celular(es) autorizado(s): $($PhysicalDevices -join ', ')" -ForegroundColor Green

Write-Host ""
Write-Host "[3/6] Gerando UM unico APK..." -ForegroundColor Yellow

if (-not $SkipBuild) {
    if ($SafeBuild) {
        Write-Host "[INFO] Build seguro: sem daemon e com 1 worker." -ForegroundColor DarkYellow
        & $Gradle assembleDebug --no-daemon --max-workers=1
    } else {
        Write-Host "[INFO] Build rapido incremental com Gradle daemon." -ForegroundColor DarkYellow
        & $Gradle assembleDebug --max-workers=2
    }

    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao compilar o APK de debug. Tente novamente com -SafeBuild."
    }
} else {
    Write-Host "[INFO] Build ignorado por parametro -SkipBuild." -ForegroundColor DarkYellow
}

if (-not (Test-Path $Apk)) {
    throw "APK nao encontrado: $Apk"
}

$ApkFile = Get-Item $Apk
$ApkHash = (Get-FileHash -Path $Apk -Algorithm SHA256).Hash

Write-Host "[OK] APK unico gerado." -ForegroundColor Green
Write-Host "Commit: $Commit"
Write-Host "APK: $($ApkFile.FullName)"
Write-Host "SHA256: $ApkHash"

Write-Host ""
Write-Host "[4/6] Atualizando Emulator..." -ForegroundColor Yellow

$DeviceLines = Get-AdbDevices
$EmulatorSerial = Get-EmulatorSerial -Lines $DeviceLines

if (-not $EmulatorSerial) {
    Write-Host "[INFO] Emulator fechado. Iniciando AVD e instalando o mesmo APK..." -ForegroundColor DarkYellow

    & $EmulatorScript
    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao iniciar/atualizar o Emulator."
    }

    $DeviceLines = Get-AdbDevices
    $EmulatorSerial = Get-EmulatorSerial -Lines $DeviceLines

    if (-not $EmulatorSerial) {
        throw "O Emulator foi iniciado, mas nao apareceu como device no ADB."
    }
} else {
    Install-And-Open -Serial $EmulatorSerial -Label "Emulator"
}

Write-Host ""
Write-Host "[5/6] Atualizando celular(es) com EXATAMENTE o mesmo APK..." -ForegroundColor Yellow

# O script que inicia o Emulator pode reiniciar o ADB.
# Por isso, os aparelhos fisicos sao validados novamente antes da instalacao.
$DeviceLines = Get-AdbDevices
$Unauthorized = Get-UnauthorizedPhysicalDevices -Lines $DeviceLines
if ($Unauthorized.Count -gt 0) {
    throw "O celular voltou como nao autorizado apos iniciar o Emulator. Confirme novamente a depuracao USB."
}

$CurrentPhysical = Get-PhysicalDevices -Lines $DeviceLines

foreach ($Target in $PhysicalDevices) {
    if ($CurrentPhysical -notcontains $Target) {
        throw "O celular '$Target' desconectou antes da instalacao. O processo foi interrompido."
    }

    Install-And-Open -Serial $Target -Label "Celular"
}

Write-Host ""
Write-Host "[6/6] Validacao final..." -ForegroundColor Yellow
Write-Host "Commit instalado em todos os dispositivos: $Commit"
Write-Host "SHA256 do APK instalado em todos: $ApkHash"

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host " EMULATOR E CELULAR ATUALIZADOS COM O MESMO APK" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "Dados locais foram preservados com 'adb install -r'."
