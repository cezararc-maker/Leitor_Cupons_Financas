param(
    [string]$Alias = "leitor-cupons-release",
    [switch]$CopyBase64ToClipboard
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $ProjectRoot

$SecretsDir = Join-Path $ProjectRoot ".secrets"
$Keystore = Join-Path $SecretsDir "leitor-cupons-release.jks"

function Find-Keytool {
    $Candidates = @()

    if ($env:JAVA_HOME) {
        $Candidates += (Join-Path $env:JAVA_HOME "bin\keytool.exe")
    }

    $Candidates += @(
        "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe",
        "C:\Program Files\Android\Android Studio\jre\bin\keytool.exe"
    )

    foreach ($Candidate in $Candidates) {
        if ($Candidate -and (Test-Path $Candidate)) {
            return $Candidate
        }
    }

    $Command = Get-Command keytool.exe -ErrorAction SilentlyContinue
    if ($Command) {
        return $Command.Source
    }

    throw "keytool.exe nao foi encontrado. Verifique a instalacao do Java/Android Studio."
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " LEITOR CUPONS - CHAVE OFICIAL DE ASSINATURA" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

$Dirty = git status --porcelain
if ($LASTEXITCODE -ne 0) {
    throw "Nao foi possivel consultar o repositorio."
}
if ($Dirty) {
    Write-Host ""
    git status --short
    throw "Existem alteracoes locais. Resolva antes de criar a chave oficial."
}

New-Item -ItemType Directory -Force -Path $SecretsDir | Out-Null

if (Test-Path $Keystore) {
    throw "A chave oficial ja existe em $Keystore. NAO gere outra chave para o mesmo aplicativo."
}

$Keytool = Find-Keytool

Write-Host ""
Write-Host "A chave sera criada em uma pasta ignorada pelo Git:" -ForegroundColor Yellow
Write-Host $Keystore
Write-Host ""
Write-Host "O keytool pedira uma senha. Guarde-a com seguranca." -ForegroundColor Yellow
Write-Host "Nao envie a senha nem o arquivo .jks por chat ou mensagem." -ForegroundColor Red
Write-Host ""

& $Keytool -genkeypair -v -keystore $Keystore -alias $Alias -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Leitor Cupons Financas, OU=Aplicativo, O=Leitor Cupons Financas, C=BR"

if ($LASTEXITCODE -ne 0 -or -not (Test-Path $Keystore)) {
    throw "A chave de assinatura nao foi criada."
}

Write-Host ""
Write-Host "[OK] Chave criada." -ForegroundColor Green
Write-Host "Alias: $Alias"
Write-Host "Arquivo: $Keystore"
Write-Host ""
Write-Host "IMPORTANTE:" -ForegroundColor Yellow
Write-Host "1. Faca uma copia offline segura deste arquivo."
Write-Host "2. Guarde as senhas em um gerenciador de senhas."
Write-Host "3. Nunca adicione o arquivo .jks ao GitHub."
Write-Host "4. Todas as releases externas devem usar esta mesma chave."

if ($CopyBase64ToClipboard) {
    $Base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($Keystore))
    Set-Clipboard -Value $Base64

    Write-Host ""
    Write-Host "[OK] Conteudo Base64 copiado para a area de transferencia." -ForegroundColor Green
    Write-Host "Cole APENAS no GitHub Secret LCF_RELEASE_KEYSTORE_BASE64." -ForegroundColor Yellow
    Write-Host "Depois substitua o conteudo da area de transferencia por outro texto."
}
