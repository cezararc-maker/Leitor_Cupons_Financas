param(
    [string]$Serial,
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

if (-not (Test-Path $Adb)) {
    throw "adb.exe nao encontrado: $Adb"
}

& $Adb start-server | Out-Null
$DeviceLines = & $Adb devices

$Unauthorized = $DeviceLines | Where-Object {
    $_ -match "^[^\s]+\s+unauthorized$" -and $_ -notmatch "^emulator-"
}

if ($Unauthorized) {
    throw "Celular conectado, mas ainda nao autorizado. Desbloqueie o celular e confirme 'Permitir depuracao USB'."
}

$PhysicalDevices = @(
    $DeviceLines |
        Where-Object {
            $_ -match "^[^\s]+\s+device$" -and $_ -notmatch "^emulator-"
        } |
        ForEach-Object {
            ($_ -split "\s+")[0]
        }
)

if ($Serial) {
    if ($PhysicalDevices -notcontains $Serial) {
        throw "O celular '$Serial' nao esta conectado ao ADB."
    }
    $Target = $Serial
} else {
    if ($PhysicalDevices.Count -eq 0) {
        throw @"
Nenhum celular Android conectado e autorizado.

No celular:
1. Ative Opcoes do desenvolvedor.
2. Ative Depuracao USB.
3. Conecte o cabo USB ao computador.
4. Confirme 'Permitir depuracao USB' no celular.
5. Execute este script novamente.
"@
    }

    if ($PhysicalDevices.Count -gt 1) {
        throw "Mais de um celular conectado. Execute novamente com -Serial <serial>."
    }

    $Target = $PhysicalDevices[0]
}

Write-Host "============================================================"
Write-Host " LEITOR CUPONS FINANCAS - INSTALAR NO CELULAR"
Write-Host "============================================================"
Write-Host ""
Write-Host "[OK] Celular conectado: $Target" -ForegroundColor Green

if (-not $SkipBuild) {
    if (-not (Test-Path $Gradle)) {
        throw "gradlew.bat nao encontrado: $Gradle"
    }

    Write-Host ""
    Write-Host "[1/3] Gerando APK de debug..." -ForegroundColor Yellow

    if ($SafeBuild) {
        & $Gradle assembleDebug --no-daemon --max-workers=1
    } else {
        & $Gradle assembleDebug --max-workers=2
    }

    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao compilar o APK de debug."
    }
} else {
    Write-Host ""
    Write-Host "[1/3] Build ignorado (-SkipBuild)." -ForegroundColor DarkYellow
}

if (-not (Test-Path $Apk)) {
    throw "APK nao encontrado: $Apk"
}

Write-Host ""
Write-Host "[2/3] Instalando APK sem apagar dados..." -ForegroundColor Yellow
& $Adb -s $Target install -r $Apk

if ($LASTEXITCODE -ne 0) {
    throw "Falha ao instalar o APK no celular."
}

Write-Host ""
Write-Host "[3/3] Abrindo aplicativo..." -ForegroundColor Yellow
& $Adb -s $Target shell am force-stop $PackageName | Out-Null
& $Adb -s $Target shell monkey -p $PackageName -c android.intent.category.LAUNCHER 1 | Out-Null

Write-Host ""
Write-Host "[OK] Aplicativo instalado e aberto no celular." -ForegroundColor Green
Write-Host "Os dados existentes do app foram preservados quando possivel."
