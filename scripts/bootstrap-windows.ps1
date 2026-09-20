param(
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $ProjectRoot

Write-Host "============================================================"
Write-Host " LEITOR CUPONS FINANCAS - PREPARACAO WINDOWS"
Write-Host "============================================================"

if (-not $env:JAVA_HOME) {
    $AndroidStudioJbr = Join-Path $env:ProgramFiles "Android\Android Studio\jbr"
    if (Test-Path (Join-Path $AndroidStudioJbr "bin\java.exe")) {
        $env:JAVA_HOME = $AndroidStudioJbr
        Write-Host "[OK] JAVA_HOME configurado pelo Android Studio."
    }
}

if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
    throw "JDK nao encontrado. Instale o Android Studio ou configure JAVA_HOME."
}

$Sdk = $env:ANDROID_HOME
if (-not $Sdk) {
    $CandidateSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
    if (Test-Path $CandidateSdk) {
        $Sdk = $CandidateSdk
        $env:ANDROID_HOME = $Sdk
        $env:ANDROID_SDK_ROOT = $Sdk
    }
}

if (-not $Sdk -or -not (Test-Path $Sdk)) {
    throw "Android SDK nao encontrado. Abra o Android Studio e instale o Android SDK antes de continuar."
}

$EscapedSdk = $Sdk.Replace("\", "\\")
Set-Content -Path "local.properties" -Value "sdk.dir=$EscapedSdk" -Encoding ASCII
Write-Host "[OK] Android SDK: $Sdk"

$SdkManager = Get-ChildItem -Path (Join-Path $Sdk "cmdline-tools") -Filter "sdkmanager.bat" -Recurse -ErrorAction SilentlyContinue | Sort-Object FullName | Select-Object -Last 1
if ($SdkManager) {
    $Platform36 = Join-Path $Sdk "platforms\android-36\android.jar"
    if (-not (Test-Path $Platform36)) {
        Write-Host ""
        Write-Host "Instalando Android SDK Platform 36..."
        cmd /c "echo y|`"$($SdkManager.FullName)`" `"platforms;android-36`" `"build-tools;36.0.0`""
        if ($LASTEXITCODE -ne 0) {
            throw "Falha ao instalar Android SDK Platform 36."
        }
    } else {
        Write-Host "[OK] Android SDK Platform 36 ja instalado."
    }
} else {
    Write-Warning "sdkmanager.bat nao localizado. Se o build reclamar do Android 36, instale-o pelo SDK Manager do Android Studio."
}

$GradleVersion = "9.6.0"

if (-not (Test-Path ".\gradlew.bat")) {
    $CacheRoot = Join-Path $env:TEMP "LeitorCuponsFinancas"
    $ZipPath = Join-Path $CacheRoot "gradle-$GradleVersion-bin.zip"
    $GradleHome = Join-Path $CacheRoot "gradle-$GradleVersion"

    New-Item -ItemType Directory -Force -Path $CacheRoot | Out-Null

    if (-not (Test-Path (Join-Path $GradleHome "bin\gradle.bat"))) {
        Write-Host "[1/3] Baixando Gradle $GradleVersion..."
        Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip" -OutFile $ZipPath
        Write-Host "[2/3] Extraindo Gradle..."
        Expand-Archive -Path $ZipPath -DestinationPath $CacheRoot -Force
    }

    Write-Host "[3/3] Criando Gradle Wrapper do projeto..."
    & (Join-Path $GradleHome "bin\gradle.bat") wrapper --gradle-version $GradleVersion --distribution-type bin
    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao gerar o Gradle Wrapper."
    }
}

Write-Host ""
Write-Host "[OK] Ambiente preparado."
& .\gradlew.bat --version
if ($LASTEXITCODE -ne 0) {
    throw "Falha ao executar o Gradle Wrapper."
}

if (-not $SkipTests) {
    Write-Host ""
    Write-Host "Executando testes e compilando APK de debug..."
    & .\gradlew.bat test assembleDebug
    if ($LASTEXITCODE -ne 0) {
        throw "Testes ou compilacao falharam."
    }

    Write-Host ""
    Write-Host "[OK] Testes e compilacao concluidos."
    $Apk = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $Apk) {
        Write-Host "[OK] APK de debug: $Apk"
    }
}

Write-Host ""
Write-Host "Projeto: $ProjectRoot"
Write-Host "O emulador e opcional nesta fase. Veja docs\EMULATOR_LITE.md."