param()

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $ProjectRoot

$Sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$SdkManager = Join-Path $Sdk "cmdline-tools\latest\bin\sdkmanager.bat"
$AvdManager = Join-Path $Sdk "cmdline-tools\latest\bin\avdmanager.bat"
$Emulator = Join-Path $Sdk "emulator\emulator.exe"

$AvdName = "LeitorCupons_API34_Lite"
$ImagePackage = "system-images;android-34;default;x86_64"

$env:ANDROID_HOME = $Sdk
$env:ANDROID_SDK_ROOT = $Sdk

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " LEITOR CUPONS FINANCAS - INSTALAR EMULADOR LITE" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

if (-not (Test-Path $SdkManager)) {
    throw "sdkmanager.bat nao encontrado em $SdkManager"
}

if (-not (Test-Path $AvdManager)) {
    throw "avdmanager.bat nao encontrado em $AvdManager"
}

$ComputerSystem = Get-CimInstance Win32_ComputerSystem
if (-not $ComputerSystem.HypervisorPresent) {
    throw "O hipervisor do Windows ainda nao esta carregado. Reinicie o Windows depois de habilitar WHPX e execute novamente."
}

Write-Host "[OK] Hipervisor do Windows carregado." -ForegroundColor Green

Write-Host ""
Write-Host "[1/4] Instalando Android Emulator e imagem Android 14 x86_64..." -ForegroundColor Yellow
& $SdkManager --sdk_root="$Sdk" "emulator" "$ImagePackage"
if ($LASTEXITCODE -ne 0) {
    throw "Falha ao instalar Android Emulator ou a imagem do sistema."
}

if (-not (Test-Path $Emulator)) {
    throw "emulator.exe nao foi encontrado apos a instalacao."
}

Write-Host ""
Write-Host "[2/4] Validando aceleracao..." -ForegroundColor Yellow
& $Emulator -accel-check
if ($LASTEXITCODE -ne 0) {
    throw "O Android Emulator nao encontrou aceleracao utilizavel, apesar do hipervisor do Windows estar carregado. Execute scripts\verificar-emulador-windows.ps1 para diagnostico."
}

Write-Host ""
Write-Host "[3/4] Criando AVD Lite..." -ForegroundColor Yellow

$AvdList = (& $AvdManager list avd 2>&1 | Out-String)
$AlreadyExists = $AvdList -match ("Name:\s*" + [regex]::Escape($AvdName))

if (-not $AlreadyExists) {
    "no" | & $AvdManager create avd -n $AvdName -k $ImagePackage -f
    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao criar o AVD $AvdName."
    }
    Write-Host "[OK] AVD criado: $AvdName" -ForegroundColor Green
} else {
    Write-Host "[OK] AVD ja existe: $AvdName" -ForegroundColor Green
}

Write-Host ""
Write-Host "[4/4] Aplicando perfil leve..." -ForegroundColor Yellow

$Config = Join-Path $env:USERPROFILE ".android\avd\$AvdName.avd\config.ini"

if (-not (Test-Path $Config)) {
    throw "config.ini do AVD nao foi encontrado em $Config"
}

function Set-AvdSetting {
    param(
        [string]$Key,
        [string]$Value
    )

    $Lines = Get-Content $Config
    $Pattern = "^" + [regex]::Escape($Key) + "="

    if ($Lines -match $Pattern) {
        $Lines = $Lines | ForEach-Object {
            if ($_ -match $Pattern) { "$Key=$Value" } else { $_ }
        }
    } else {
        $Lines += "$Key=$Value"
    }

    Set-Content -Path $Config -Value $Lines -Encoding ASCII
}

Set-AvdSetting "hw.ramSize" "1536"
Set-AvdSetting "hw.cpu.ncore" "2"
Set-AvdSetting "hw.lcd.width" "720"
Set-AvdSetting "hw.lcd.height" "1280"
Set-AvdSetting "hw.lcd.density" "320"
Set-AvdSetting "hw.camera.back" "none"
Set-AvdSetting "hw.camera.front" "none"
Set-AvdSetting "showDeviceFrame" "no"
Set-AvdSetting "disk.dataPartition.size" "2G"

Write-Host ""
Write-Host "[OK] Emulador Lite configurado." -ForegroundColor Green
Write-Host "AVD: $AvdName"
Write-Host "Android: 14 / API 34"
Write-Host "Imagem: AOSP x86_64"
Write-Host "RAM: 1536 MB"
Write-Host "CPU: 2 cores"
Write-Host "Resolucao: 720 x 1280"
Write-Host ""
Write-Host "Proximo passo: scripts\executar-app-emulador.ps1"
