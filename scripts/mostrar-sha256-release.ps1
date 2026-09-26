param(
    [string]$KeystorePath = "",
    [string]$Alias = "leitor-cupons-release"
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $ProjectRoot

if ([string]::IsNullOrWhiteSpace($KeystorePath)) {
    $KeystorePath = Join-Path $ProjectRoot ".secrets\leitor-cupons-release.jks"
}

if (-not (Test-Path $KeystorePath)) {
    throw "Keystore nao encontrado: $KeystorePath"
}

$KeytoolCommand = Get-Command keytool.exe -ErrorAction SilentlyContinue

if (-not $KeytoolCommand -and $env:JAVA_HOME) {
    $Candidate = Join-Path $env:JAVA_HOME "bin\keytool.exe"
    if (Test-Path $Candidate) {
        $KeytoolCommand = Get-Item $Candidate
    }
}

if (-not $KeytoolCommand) {
    throw "keytool.exe nao encontrado. Verifique a instalacao do Java/JDK 17."
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " ASSINATURA RELEASE - SHA-256" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Keystore: $KeystorePath"
Write-Host "Alias: $Alias"
Write-Host ""
Write-Host "O keytool podera pedir a senha do keystore." -ForegroundColor Yellow
Write-Host "Digite-a apenas no prompt local. NAO envie a senha ao chat." -ForegroundColor Yellow
Write-Host ""

$Output = & $KeytoolCommand.Source -list -v -keystore $KeystorePath -alias $Alias 2>&1
$ExitCode = $LASTEXITCODE

if ($ExitCode -ne 0) {
    $Output | ForEach-Object { Write-Host $_ }
    throw "Nao foi possivel consultar o certificado da chave release."
}

$Sha256 = @(
    $Output |
        Select-String -Pattern "SHA256:"
)

if ($Sha256.Count -eq 0) {
    Write-Host "Saida do keytool:" -ForegroundColor Yellow
    $Output | ForEach-Object { Write-Host $_ }
    throw "Impressao digital SHA-256 nao localizada."
}

Write-Host "SHA-256 encontrado:" -ForegroundColor Green
$Sha256 | ForEach-Object { Write-Host $_.Line.Trim() }

Write-Host ""
Write-Host "A impressao SHA-256 do certificado pode ser cadastrada no Firebase App Check." -ForegroundColor Cyan
Write-Host "Nao confunda a impressao digital com a senha ou a chave privada." -ForegroundColor DarkCyan
