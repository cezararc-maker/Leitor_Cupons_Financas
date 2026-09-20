param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $ProjectRoot

function Test-WingetPackageInstalled {
    param([Parameter(Mandatory = $true)][string]$PackageId)

    $Output = (& winget list --exact --id $PackageId --accept-source-agreements 2>&1 | Out-String)
    return $Output -match [regex]::Escape($PackageId)
}

function Ensure-Jdk17 {
    if (Test-WingetPackageInstalled -PackageId "Microsoft.OpenJDK.17") {
        Write-Host "[OK] Microsoft OpenJDK 17 ja instalado." -ForegroundColor Green
        return
    }

    & winget install --exact --id Microsoft.OpenJDK.17 --accept-package-agreements --accept-source-agreements

    if (-not (Test-WingetPackageInstalled -PackageId "Microsoft.OpenJDK.17")) {
        throw "Falha ao instalar/verificar Microsoft OpenJDK 17."
    }
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " LEITOR CUPONS FINANCAS - AMBIENTE MINIMO WINDOWS" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
    throw "winget nao encontrado. Instale/atualize o App Installer da Microsoft Store."
}

Write-Host ""
Write-Host "[1/4] Verificando Microsoft OpenJDK 17..." -ForegroundColor Yellow
Ensure-Jdk17

$MicrosoftJdk = Get-ChildItem -Path (Join-Path $env:ProgramFiles "Microsoft") -Directory -Filter "jdk-17*" -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $MicrosoftJdk) {
    throw "Microsoft OpenJDK 17 esta instalado, mas a pasta do JDK nao foi localizada."
}

$env:JAVA_HOME = $MicrosoftJdk.FullName
$env:Path = "$($env:JAVA_HOME)\bin;$env:Path"
[Environment]::SetEnvironmentVariable("JAVA_HOME", $env:JAVA_HOME, "User")

$Sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
New-Item -ItemType Directory -Force -Path $Sdk | Out-Null

$env:ANDROID_HOME = $Sdk
$env:ANDROID_SDK_ROOT = $Sdk
[Environment]::SetEnvironmentVariable("ANDROID_HOME", $Sdk, "User")
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", $Sdk, "User")

Write-Host ""
Write-Host "[2/4] Verificando Android SDK Command-Line Tools..." -ForegroundColor Yellow

$SdkManager = Join-Path $Sdk "cmdline-tools\latest\bin\sdkmanager.bat"

if (-not (Test-Path $SdkManager)) {
    Write-Host ""
    Write-Host "Android SDK Command-Line Tools ainda nao foram encontradas." -ForegroundColor Yellow
    Write-Host "Baixe o pacote oficial 'Command line tools only' para Windows e extraia de forma que exista:" -ForegroundColor Yellow
    Write-Host $SdkManager -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Depois execute este script novamente." -ForegroundColor Yellow
    throw "sdkmanager.bat nao encontrado."
}

Write-Host "[OK] sdkmanager: $SdkManager" -ForegroundColor Green

Write-Host ""
Write-Host "[3/4] Aceitando licencas e instalando pacotes do SDK..." -ForegroundColor Yellow

1..30 | ForEach-Object { "y" } | & $SdkManager --sdk_root="$Sdk" --licenses
if ($LASTEXITCODE -ne 0) {
    Write-Warning "O comando de licencas retornou codigo $LASTEXITCODE. A instalacao dos pacotes ainda sera tentada."
}

& $SdkManager --sdk_root="$Sdk" "platform-tools" "platforms;android-36" "build-tools;35.0.0"
if ($LASTEXITCODE -ne 0) {
    throw "Falha ao instalar os pacotes do Android SDK pelo sdkmanager."
}

$EscapedSdk = $Sdk.Replace("\", "\\")
Set-Content -Path (Join-Path $ProjectRoot "local.properties") -Value "sdk.dir=$EscapedSdk" -Encoding ASCII

Write-Host ""
Write-Host "[OK] JAVA_HOME: $env:JAVA_HOME"
Write-Host "[OK] ANDROID_HOME: $env:ANDROID_HOME"
Write-Host "[OK] Platform 36: $(Test-Path (Join-Path $Sdk 'platforms\android-36\android.jar'))"
Write-Host "[OK] Build Tools 35.0.0: $(Test-Path (Join-Path $Sdk 'build-tools\35.0.0'))"
Write-Host "[OK] Platform Tools: $(Test-Path (Join-Path $Sdk 'platform-tools\adb.exe'))"

Write-Host ""
Write-Host "[4/4] Validando projeto..." -ForegroundColor Yellow
& (Join-Path $env:JAVA_HOME "bin\java.exe") -version
& $SdkManager --version

if (-not $SkipBuild) {
    & (Join-Path $ProjectRoot "scripts\bootstrap-windows.ps1")
    if ($LASTEXITCODE -ne 0) {
        throw "Falha na validacao do projeto."
    }
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host " AMBIENTE MINIMO CONFIGURADO" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "Android Studio e emulador continuam opcionais."
