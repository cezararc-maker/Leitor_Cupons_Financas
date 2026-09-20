param()

$ErrorActionPreference = "Continue"

$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $ProjectRoot

$Sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$env:ANDROID_HOME = $Sdk
$env:ANDROID_SDK_ROOT = $Sdk

$LogDir = Join-Path $ProjectRoot "data\runtime\reports"
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
$Stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$Log = Join-Path $LogDir "android_sdk_diagnostico_$Stamp.txt"

function Write-Section {
    param([string]$Title)
    "" | Tee-Object -FilePath $Log -Append
    "============================================================" | Tee-Object -FilePath $Log -Append
    $Title | Tee-Object -FilePath $Log -Append
    "============================================================" | Tee-Object -FilePath $Log -Append
}

function Run-Android {
    param(
        [string]$Label,
        [string[]]$AndroidArgs
    )

    Write-Section $Label
    ("COMANDO: android " + ($AndroidArgs -join " ")) | Tee-Object -FilePath $Log -Append

    & android @AndroidArgs 2>&1 | Tee-Object -FilePath $Log -Append
    $Code = $LASTEXITCODE

    ("EXITCODE: " + $Code) | Tee-Object -FilePath $Log -Append
    return $Code
}

Write-Section "AMBIENTE"

"Windows:" | Tee-Object -FilePath $Log -Append
Get-CimInstance Win32_OperatingSystem |
    Select-Object Caption, Version, OSArchitecture |
    Format-List |
    Out-String |
    Tee-Object -FilePath $Log -Append

("JAVA_HOME=" + $env:JAVA_HOME) | Tee-Object -FilePath $Log -Append
("ANDROID_HOME=" + $env:ANDROID_HOME) | Tee-Object -FilePath $Log -Append
("ANDROID_SDK_ROOT=" + $env:ANDROID_SDK_ROOT) | Tee-Object -FilePath $Log -Append

"where android:" | Tee-Object -FilePath $Log -Append
(& where.exe android 2>&1 | Out-String) | Tee-Object -FilePath $Log -Append

Run-Android -Label "VERSAO" -AndroidArgs @("--version") | Out-Null
Run-Android -Label "INFO" -AndroidArgs @("--sdk=$Sdk", "info") | Out-Null
Run-Android -Label "HELP SDK INSTALL" -AndroidArgs @("sdk", "install", "-h") | Out-Null
Run-Android -Label "UPDATE CLI" -AndroidArgs @("update") | Out-Null

Run-Android -Label "LISTA PLATFORM 36" -AndroidArgs @("--sdk=$Sdk", "-v", "sdk", "list", "platforms/android-36", "--all-versions") | Out-Null
Run-Android -Label "LISTA BUILD TOOLS 35" -AndroidArgs @("--sdk=$Sdk", "-v", "sdk", "list", "build-tools/35.0.0", "--all-versions") | Out-Null
Run-Android -Label "LISTA PLATFORM TOOLS" -AndroidArgs @("--sdk=$Sdk", "-v", "sdk", "list", "platform-tools", "--all-versions") | Out-Null

$Failures = @()
$Packages = @(
    "platforms/android-36",
    "build-tools/35.0.0",
    "platform-tools"
)

foreach ($Package in $Packages) {
    $Code = Run-Android -Label ("INSTALACAO " + $Package) -AndroidArgs @("--sdk=$Sdk", "-v", "sdk", "install", $Package)
    if ($Code -ne 0) {
        $Failures += $Package
    }
}

Write-Section "RESULTADO"
if ($Failures.Count -eq 0) {
    "[OK] Todos os pacotes foram instalados pela Android CLI." | Tee-Object -FilePath $Log -Append
} else {
    ("[FALHA] Pacotes com erro: " + ($Failures -join ", ")) | Tee-Object -FilePath $Log -Append
}

("Relatorio: " + $Log) | Tee-Object -FilePath $Log -Append

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " DIAGNOSTICO CONCLUIDO" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Relatorio: $Log"