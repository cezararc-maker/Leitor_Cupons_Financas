param(
    [Parameter(Mandatory = $true)]
    [string]$ImagePath
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $ProjectRoot

$Sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$Adb = Join-Path $Sdk "platform-tools\adb.exe"
$UpdateScript = Join-Path $ProjectRoot "scripts\atualizar-app-emulador.ps1"

if (-not (Test-Path $Adb)) {
    throw "adb.exe nao encontrado: $Adb"
}

$ResolvedImage = (Resolve-Path $ImagePath).Path
if (-not (Test-Path $ResolvedImage -PathType Leaf)) {
    throw "Imagem nao encontrada: $ImagePath"
}

$Extension = [System.IO.Path]::GetExtension($ResolvedImage)
$BaseName = [System.IO.Path]::GetFileNameWithoutExtension($ResolvedImage)
$SafeName = ($BaseName -replace '[^A-Za-z0-9_-]', '_') + $Extension
$RemotePath = "/sdcard/Download/$SafeName"

& $Adb start-server | Out-Null
$Devices = (& $Adb devices | Out-String)

if ($Devices -notmatch "(?m)^(emulator-\d+)\s+device\b") {
    if (-not (Test-Path $UpdateScript)) {
        throw "Emulator fechado e script de inicializacao nao encontrado."
    }

    Write-Host "[INFO] Emulator fechado. Abrindo automaticamente..." -ForegroundColor DarkYellow
    & $UpdateScript -SkipBuild

    if ($LASTEXITCODE -ne 0) {
        throw "Nao foi possivel iniciar o Emulator."
    }

    & $Adb start-server | Out-Null
    $Devices = (& $Adb devices | Out-String)

    if ($Devices -notmatch "(?m)^(emulator-\d+)\s+device\b") {
        throw "Emulator foi iniciado, mas ainda nao esta disponivel no ADB."
    }
}

$Serial = $Matches[1]

Write-Host ""
Write-Host "Enviando imagem para o Emulator..." -ForegroundColor Yellow
& $Adb -s $Serial push $ResolvedImage $RemotePath

if ($LASTEXITCODE -ne 0) {
    throw "Falha ao enviar a imagem para o Emulator."
}

Write-Host ""
Write-Host "[OK] Imagem enviada." -ForegroundColor Green
Write-Host "No app, toque em 'Selecionar imagem com QR Code'."
Write-Host "Depois abra a pasta Download e escolha:"
Write-Host "  $SafeName"
