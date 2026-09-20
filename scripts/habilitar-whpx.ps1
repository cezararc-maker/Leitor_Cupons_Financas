param()

$ErrorActionPreference = "Stop"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " LEITOR CUPONS FINANCAS - HABILITAR WHPX" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

$IsAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole(
    [Security.Principal.WindowsBuiltInRole]::Administrator
)

if (-not $IsAdmin) {
    throw "Abra o PowerShell como Administrador e execute este script novamente."
}

$Cpu = Get-CimInstance Win32_Processor | Select-Object -First 1

if (-not $Cpu.VirtualizationFirmwareEnabled) {
    throw "A virtualizacao ainda esta desabilitada no BIOS/UEFI."
}

Write-Host ""
Write-Host "[1/2] Virtualizacao do processador..." -ForegroundColor Yellow
Write-Host "[OK] VT-x no firmware: $($Cpu.VirtualizationFirmwareEnabled)" -ForegroundColor Green
Write-Host "[OK] VM Monitor: $($Cpu.VMMonitorModeExtensions)" -ForegroundColor Green
Write-Host "[OK] SLAT/EPT: $($Cpu.SecondLevelAddressTranslationExtensions)" -ForegroundColor Green

Write-Host ""
Write-Host "[2/2] Windows Hypervisor Platform..." -ForegroundColor Yellow

$Feature = Get-WindowsOptionalFeature -Online -FeatureName HypervisorPlatform

if ($Feature.State -eq "Enabled") {
    Write-Host "[OK] Windows Hypervisor Platform ja esta habilitada." -ForegroundColor Green
    Write-Host "Pode seguir para scripts\instalar-emulador-lite.ps1."
    exit 0
}

$Result = Enable-WindowsOptionalFeature -Online -FeatureName HypervisorPlatform -All -NoRestart

Write-Host ""
if ($Result.RestartNeeded) {
    Write-Host "[OK] Windows Hypervisor Platform habilitada." -ForegroundColor Green
    Write-Host "[ACAO NECESSARIA] Reinicie o Windows antes de instalar/iniciar o emulador." -ForegroundColor Yellow
} else {
    Write-Host "[OK] Windows Hypervisor Platform habilitada sem reinicio pendente." -ForegroundColor Green
}
