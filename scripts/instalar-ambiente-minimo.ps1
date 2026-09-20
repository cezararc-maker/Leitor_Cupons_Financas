param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $ProjectRoot

function Refresh-SessionPath {
    $MachinePath = [Environment]::GetEnvironmentVariable("Path", "Machine")
    $UserPath = [Environment]::GetEnvironmentVariable("Path", "User")
    $env:Path = "$MachinePath;$UserPath"
}

function Test-WingetPackageInstalled {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PackageId
    )

    $Output = (& winget list --exact --id $PackageId --accept-source-agreements 2>&1 | Out-String)
    return $Output -match [regex]::Escape($PackageId)
}

function Ensure-WingetPackage {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PackageId,

        [Parameter(Mandatory = $true)]
        [string]$Label
    )

    if (Test-WingetPackageInstalled -PackageId $PackageId) {
        Write-Host "[OK] $Label ja instalado." -ForegroundColor Green
        return
    }

    Write-Host "Instalando $Label..."
    & winget install --exact --id $PackageId --accept-package-agreements --accept-source-agreements

    # Alguns pacotes do WinGet podem retornar codigo nao-zero mesmo apos
    # uma instalacao/registro bem-sucedido. Sempre confirme pelo inventario.
    if (-not (Test-WingetPackageInstalled -PackageId $PackageId)) {
        throw "Falha ao instalar/verificar $Label."
    }

    Write-Host "[OK] $Label instalado." -ForegroundColor Green
}

function Resolve-AndroidCli {
    Refresh-SessionPath

    $Command = Get-Command android -ErrorAction SilentlyContinue
    if ($Command) {
        if ($Command.Source) { return $Command.Source }
        if ($Command.Path) { return $Command.Path }
    }

    $WhereResult = & where.exe android 2>$null
    if ($LASTEXITCODE -eq 0 -and $WhereResult) {
        return ($WhereResult | Select-Object -First 1)
    }

    $Candidates = @(
        (Join-Path $env:LOCALAPPDATA "Microsoft\WinGet\Links\android.exe"),
        (Join-Path $env:LOCALAPPDATA "Microsoft\WindowsApps\android.exe")
    )

    foreach ($Candidate in $Candidates) {
        if (Test-Path $Candidate) {
            return $Candidate
        }
    }

    $SearchRoots = @(
        (Join-Path $env:LOCALAPPDATA "Microsoft\WinGet"),
        (Join-Path $env:LOCALAPPDATA "Packages")
    )

    foreach ($Root in $SearchRoots) {
        if (Test-Path $Root) {
            $Found = Get-ChildItem -Path $Root -Filter "android.exe" -File -Recurse -Force -ErrorAction SilentlyContinue |
                Select-Object -First 1

            if ($Found) {
                return $Found.FullName
            }
        }
    }

    return $null
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " LEITOR CUPONS FINANCAS - AMBIENTE MINIMO WINDOWS" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
    throw "winget nao encontrado. Instale/atualize o App Installer da Microsoft Store."
}

Write-Host ""
Write-Host "[1/4] Instalando/verificando Microsoft OpenJDK 17..." -ForegroundColor Yellow
Ensure-WingetPackage -PackageId "Microsoft.OpenJDK.17" -Label "Microsoft OpenJDK 17"

Write-Host ""
Write-Host "[2/4] Instalando/verificando Android CLI..." -ForegroundColor Yellow
Ensure-WingetPackage -PackageId "Google.AndroidCLI" -Label "Android CLI"

Refresh-SessionPath

$MicrosoftJdk = Get-ChildItem -Path (Join-Path $env:ProgramFiles "Microsoft") -Directory -Filter "jdk-17*" -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $MicrosoftJdk) {
    throw "Microsoft OpenJDK 17 esta instalado, mas a pasta do JDK nao foi localizada."
}

$env:JAVA_HOME = $MicrosoftJdk.FullName
$env:Path = "$($env:JAVA_HOME)\bin;$env:Path"
[Environment]::SetEnvironmentVariable("JAVA_HOME", $env:JAVA_HOME, "User")

$AndroidExe = Resolve-AndroidCli

if (-not $AndroidExe) {
    Write-Host ""
    Write-Host "[ATENCAO] O Android CLI consta como instalado no WinGet, mas o alias ainda nao esta visivel nesta sessao." -ForegroundColor Yellow
    Write-Host "Feche esta janela do PowerShell, abra uma nova e execute novamente o mesmo script." -ForegroundColor Yellow
    Write-Host "Depois da reabertura, o comando 'android' deve estar disponivel." -ForegroundColor Yellow
    throw "Android CLI instalado, mas android.exe ainda nao ficou disponivel nesta sessao."
}

Write-Host "[OK] Android CLI: $AndroidExe" -ForegroundColor Green

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
