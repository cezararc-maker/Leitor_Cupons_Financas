param()

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $ProjectRoot

$Sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$Emulator = Join-Path $Sdk "emulator\emulator.exe"
$Adb = Join-Path $Sdk "platform-tools\adb.exe"
$AvdName = "LeitorCupons_API34_Lite"
$PackageName = "br.com.leitorcuponsfinancas"
$Apk = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " LEITOR CUPONS FINANCAS - EXECUTAR NO EMULADOR" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

foreach ($Required in @($Emulator, $Adb, $Apk)) {
    if (-not (Test-Path $Required)) {
        throw "Arquivo necessario nao encontrado: $Required"
    }
}

Write-Host ""
Write-Host "[1/4] Verificando emulador..." -ForegroundColor Yellow

& $Adb start-server | Out-Null
$Devices = (& $Adb devices | Out-String)

if ($Devices -notmatch "emulator-\d+\s+device") {
    Write-Host "Iniciando $AvdName..."
    Start-Process -FilePath $Emulator -ArgumentList @(
        "@$AvdName",
        "-gpu", "auto",
        "-no-audio",
        "-no-boot-anim",
        "-memory", "1536",
        "-cores", "2"
    )
}

Write-Host ""
Write-Host "[2/4] Aguardando Android concluir a inicializacao..." -ForegroundColor Yellow

$Ready = $false
$Serial = $null

for ($Attempt = 1; $Attempt -le 120; $Attempt++) {
    Start-Sleep -Seconds 2

    $DevicesText = (& $Adb devices | Out-String)

    if ($DevicesText -match "(?m)^(emulator-\d+)\s+device\s*$") {
        $Serial = $Matches[1]

        $Boot = (& $Adb -s $Serial shell getprop sys.boot_completed 2>$null | Out-String).Trim()

        if ($Boot -eq "1") {
            $Ready = $true
            break
        }
    }

    if (($Attempt % 10) -eq 0) {
        Write-Host "Aguardando emulador... tentativa $Attempt/120" -ForegroundColor DarkYellow
    }
}

if (-not $Ready -or -not $Serial) {
    throw "O Android nao concluiu a inicializacao dentro do limite de verificacao."
}

Write-Host "[OK] Android iniciado: $Serial" -ForegroundColor Green

Write-Host ""
Write-Host "[3/4] Instalando APK..." -ForegroundColor Yellow
& $Adb -s $Serial install -r $Apk
if ($LASTEXITCODE -ne 0) {
    throw "Falha ao instalar o APK no emulador."
}

Write-Host ""
Write-Host "[4/4] Abrindo Leitor Cupons Financas..." -ForegroundColor Yellow
& $Adb -s $Serial shell am force-stop $PackageName
& $Adb -s $Serial shell am start -n "$PackageName/.MainActivity"

if ($LASTEXITCODE -ne 0) {
    throw "APK instalado, mas o app nao pode ser iniciado automaticamente."
}

Write-Host ""
Write-Host "[OK] Leitor Cupons Financas aberto no emulador." -ForegroundColor Green
