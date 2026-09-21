param(
    [string]$Serial,
    [switch]$SkipBuild,
    [switch]$SafeBuild
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$Unified = Join-Path $ScriptDir "atualizar-app-todos.ps1"

if (-not (Test-Path $Unified)) {
    throw "Script unificado nao encontrado: $Unified"
}

Write-Host "[INFO] O fluxo agora atualiza Emulator + celular com o mesmo APK." -ForegroundColor Cyan

& $Unified -PhoneSerial $Serial -SkipBuild:$SkipBuild -SafeBuild:$SafeBuild

if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
