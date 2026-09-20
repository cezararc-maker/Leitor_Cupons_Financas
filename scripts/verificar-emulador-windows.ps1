param()

$ErrorActionPreference = "Continue"

$Sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " LEITOR CUPONS FINANCAS - DIAGNOSTICO DO EMULADOR" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

Write-Host ""
Write-Host "[1/4] Processador e virtualizacao..." -ForegroundColor Yellow
Get-CimInstance Win32_Processor |
    Select-Object Name, VirtualizationFirmwareEnabled, VMMonitorModeExtensions, SecondLevelAddressTranslationExtensions |
    Format-List

Write-Host ""
Write-Host "[2/4] Placa-mae e BIOS..." -ForegroundColor Yellow
Get-CimInstance Win32_BaseBoard |
    Select-Object Manufacturer, Product, Version |
    Format-List

Get-CimInstance Win32_BIOS |
    Select-Object Manufacturer, SMBIOSBIOSVersion, ReleaseDate |
    Format-List

Write-Host ""
Write-Host "[3/4] Recursos do Windows..." -ForegroundColor Yellow
Get-WindowsOptionalFeature -Online -FeatureName HypervisorPlatform -ErrorAction SilentlyContinue |
    Select-Object FeatureName, State |
    Format-List

Get-WindowsOptionalFeature -Online -FeatureName VirtualMachinePlatform -ErrorAction SilentlyContinue |
    Select-Object FeatureName, State |
    Format-List

Write-Host ""
Write-Host "[4/4] Android Emulator..." -ForegroundColor Yellow

$Emulator = Join-Path $Sdk "emulator\emulator.exe"
$AvdManager = Join-Path $Sdk "cmdline-tools\latest\bin\avdmanager.bat"

Write-Host "SDK: $Sdk"
Write-Host "emulator.exe existe: $(Test-Path $Emulator)"
Write-Host "avdmanager.bat existe: $(Test-Path $AvdManager)"

if (Test-Path $Emulator) {
    Write-Host ""
    Write-Host "Verificacao de aceleracao:" -ForegroundColor Yellow
    & $Emulator -accel-check
}

Write-Host ""
Write-Host "Nenhuma instalacao foi realizada por este script." -ForegroundColor Green
