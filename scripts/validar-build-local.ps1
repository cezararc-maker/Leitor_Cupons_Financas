param()

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $ProjectRoot

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " LEITOR CUPONS FINANCAS - BUILD LOCAL LIMPO" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

if (-not (Test-Path ".\gradlew.bat")) {
    throw "gradlew.bat nao encontrado. Execute scripts\bootstrap-windows.ps1 antes."
}

Write-Host ""
Write-Host "[1/4] Encerrando daemons Gradle..." -ForegroundColor Yellow
& .\gradlew.bat --stop
Start-Sleep -Seconds 2

Write-Host ""
Write-Host "[2/4] Limpando artefatos anteriores..." -ForegroundColor Yellow

$BuildDir = Join-Path $ProjectRoot "app\build"

if (Test-Path $BuildDir) {
    $Removed = $false

    for ($Attempt = 1; $Attempt -le 5; $Attempt++) {
        try {
            Remove-Item -Path $BuildDir -Recurse -Force -ErrorAction Stop
            $Removed = $true
            break
        } catch {
            Write-Host "Tentativa $Attempt/5: arquivo ainda bloqueado; aguardando..." -ForegroundColor DarkYellow
            Start-Sleep -Seconds 2
        }
    }

    if (-not $Removed -and (Test-Path $BuildDir)) {
        throw "Nao foi possivel remover app\build. Feche janelas do Explorer/VS Code que estejam visualizando a pasta e execute novamente."
    }
}

Write-Host "[OK] Build anterior removido." -ForegroundColor Green

Write-Host ""
Write-Host "[3/4] Executando testes de debug e compilacao com um worker..." -ForegroundColor Yellow
& .\gradlew.bat clean testDebugUnitTest assembleDebug --no-daemon --max-workers=1

if ($LASTEXITCODE -ne 0) {
    throw "Testes ou compilacao falharam."
}

Write-Host ""
Write-Host "[4/4] Validando APK novo..." -ForegroundColor Yellow

$Apk = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"

if (-not (Test-Path $Apk)) {
    throw "Build terminou sem gerar o APK esperado."
}

$ApkFile = Get-Item $Apk
Write-Host "[OK] APK gerado nesta compilacao:" -ForegroundColor Green
Write-Host "Caminho: $($ApkFile.FullName)"
Write-Host "Tamanho: $([math]::Round($ApkFile.Length / 1MB, 2)) MB"
Write-Host "Gerado em: $($ApkFile.LastWriteTime)"

Write-Host ""
Write-Host "Estado do Git:" -ForegroundColor Yellow
git status --short
