param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $ProjectRoot

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " LEITOR CUPONS FINANCAS - AMBIENTE MINIMO WINDOWS" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
    throw "winget nao encontrado. Instale/atualize o App Installer da Microsoft Store."
}

Write-Host ""
Write-Host "[1/4] Instalando/verificando Microsoft OpenJDK 17..." -ForegroundColor Yellow
winget install --exact --id Microsoft.OpenJDK.17 --accept-package-agreements --accept-source-agreements
if ($LASTEXITCODE -ne 0) {
    throw "Falha ao instalar/verificar Microsoft OpenJDK 17."
}

Write-Host ""
Write-Host "[2/4] Instalando/verificando Android CLI..." -ForegroundColor Yellow
winget install --exact --id Google.AndroidCLI --accept-package-agreements --accept-source-agreements
if ($LASTEXITCODE -ne 0) {
    throw "Falha ao instalar/verificar Android CLI."
}

$MicrosoftJdk = Get-ChildItem -Path (Join-Path $env:ProgramFiles "Microsoft") -Directory -Filter "jdk-17*" -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $MicrosoftJdk) {
    throw "Microsoft OpenJDK 17 foi instalado, mas a pasta do JDK nao foi localizada."
}

$env:JAVA_HOME = $MicrosoftJdk.FullName
$env:Path = "$($env:JAVA_HOME)\bin;$env:LOCALAPPDATA\Microsoft\WinGet\Links;$env:Path"

[Environment]::SetEnvironmentVariable("JAVA_HOME", $env:JAVA_HOME, "User")

$AndroidExe = $null
$AndroidCommand = Get-Command android -ErrorAction SilentlyContinue
if ($AndroidCommand) {
    $AndroidExe = $AndroidCommand.Source
}

if (-not $AndroidExe) {
    $WingetAndroid = Join-Path $env:LOCALAPPDATA "Microsoft\WinGet\Links\android.exe"
    if (Test-Path $WingetAndroid) {
        $AndroidExe = $WingetAndroid
    }
}

if (-not $AndroidExe) {
    throw "Android CLI foi instalado, mas android.exe nao foi localizado. Feche e reabra o PowerShell e execute novamente."
}

$Sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
New-Item -ItemType Directory -Force -Path $Sdk | Out-Null

$env:ANDROID_HOME = $Sdk
$env:ANDROID_SDK_ROOT = $Sdk
[Environment]::SetEnvironmentVariable("ANDROID_HOME", $Sdk, "User")
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", $Sdk, "User")

Write-Host ""
Write-Host "[3/4] Instalando Android SDK necessario ao projeto..." -ForegroundColor Yellow
& $AndroidExe --sdk="$Sdk" sdk install platforms/android-36 build-tools/35.0.0 platform-tools
if ($LASTEXITCODE -ne 0) {
    throw "Falha ao instalar os pacotes do Android SDK."
}

$EscapedSdk = $Sdk.Replace("\", "\\")
Set-Content -Path (Join-Path $ProjectRoot "local.properties") -Value "sdk.dir=$EscapedSdk" -Encoding ASCII

Write-Host ""
Write-Host "[OK] JAVA_HOME: $env:JAVA_HOME"
Write-Host "[OK] ANDROID_HOME: $env:ANDROID_HOME"

Write-Host ""
Write-Host "[4/4] Validando ferramentas..." -ForegroundColor Yellow
& (Join-Path $env:JAVA_HOME "bin\java.exe") -version
& $AndroidExe info

if (-not $SkipBuild) {
    Write-Host ""
    Write-Host "Executando bootstrap, testes e compilacao..." -ForegroundColor Yellow
    & (Join-Path $ProjectRoot "scripts\bootstrap-windows.ps1")
    if ($LASTEXITCODE -ne 0) {
        throw "Falha na validacao do projeto."
    }
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host " AMBIENTE MINIMO CONFIGURADO" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "Nao foi instalado Android Studio nem emulador."
