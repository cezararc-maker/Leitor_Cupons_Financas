param(
    [switch]$SkipBuild,
    [switch]$SafeBuild
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $ProjectRoot

$Sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$Adb = Join-Path $Sdk "platform-tools\adb.exe"
$Apk = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"
$PackageName = "br.com.leitorcuponsfinancas"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " LEITOR CUPONS FINANCAS - ATUALIZAR APP NO EMULADOR" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

if (-not (Test-Path $Adb)) {
    throw "adb.exe nao encontrado: $Adb"
}

Write-Host ""
Write-Host "[1/4] Localizando emulador ativo..." -ForegroundColor Yellow

& $Adb start-server | Out-Null
$Devices = (& $Adb devices | Out-String)

if ($Devices -notmatch "(?m)^(emulator-\d+)\s+device\b") {
    throw "Nenhum Android Emulator ativo e conectado ao ADB. Abra o emulador primeiro."
}

$Serial = $Matches[1]
Write-Host "[OK] Emulador conectado: $Serial" -ForegroundColor Green

Write-Host ""
Write-Host "[2/4] Gerando APK de debug..." -ForegroundColor Yellow

if ($SkipBuild) {
    Write-Host "[INFO] Build ignorado por parametro -SkipBuild." -ForegroundColor DarkYellow
} else {
    if (-not (Test-Path ".\gradlew.bat")) {
        throw "gradlew.bat nao encontrado."
    }

    if ($SafeBuild) {
        Write-Host "[INFO] Build seguro: sem daemon e com 1 worker." -ForegroundColor DarkYellow
        & .\gradlew.bat assembleDebug --no-daemon --max-workers=1
    } else {
        Write-Host "[INFO] Build rapido incremental com Gradle daemon." -ForegroundColor DarkYellow
        & .\gradlew.bat assembleDebug --max-workers=2
    }

    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao compilar o APK de debug. Se houver bloqueio de arquivo, tente novamente com -SafeBuild."
    }
}

if (-not (Test-Path $Apk)) {
    throw "APK nao encontrado: $Apk"
}

Write-Host ""
Write-Host "[3/4] Atualizando APK sem apagar dados..." -ForegroundColor Yellow

& $Adb -s $Serial install -r $Apk

if ($LASTEXITCODE -ne 0) {
    throw "Falha ao instalar o APK atualizado no emulador."
}

Write-Host ""
Write-Host "[4/4] Reiniciando somente o aplicativo..." -ForegroundColor Yellow

& $Adb -s $Serial shell am force-stop $PackageName
& $Adb -s $Serial shell am start -n "$PackageName/.MainActivity"

if ($LASTEXITCODE -ne 0) {
    throw "APK atualizado, mas o aplicativo nao abriu automaticamente."
}

$ApkFile = Get-Item $Apk

Write-Host ""
Write-Host "[OK] Leitor Cupons Financas atualizado no emulador." -ForegroundColor Green
Write-Host "APK: $($ApkFile.FullName)"
Write-Host "Gerado em: $($ApkFile.LastWriteTime)"
Write-Host "Dados do aplicativo preservados."
