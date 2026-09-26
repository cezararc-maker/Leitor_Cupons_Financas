param(
    [string]$PhoneSerial = "ZF52554B2L"
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $ProjectRoot

$Sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$Adb = Join-Path $Sdk "platform-tools\adb.exe"
$PackageName = "br.com.leitorcuponsfinancas"

if (-not (Test-Path $Adb)) {
    throw "ADB nao encontrado: $Adb"
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " FIREBASE APP CHECK - TOKEN DEBUG" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Este script NAO altera configuracoes do Android." -ForegroundColor DarkCyan
Write-Host "O token exibido deve ser cadastrado no Firebase e mantido privado." -ForegroundColor Yellow
Write-Host "NAO envie o token ao chat e NAO versione no GitHub." -ForegroundColor Yellow

& $Adb start-server | Out-Null

$Devices = @(
    & $Adb devices |
        Where-Object {
            $_ -match "^[^\s]+\s+device$" -and
            $_ -notmatch "^emulator-"
        } |
        ForEach-Object {
            ($_ -split "\s+")[0]
        }
)

if ($Devices -notcontains $PhoneSerial) {
    & $Adb devices -l
    throw "O celular '$PhoneSerial' nao esta conectado/autorizado no ADB."
}

Write-Host ""
Write-Host "[1/3] Limpando apenas o logcat..." -ForegroundColor Yellow
& $Adb -s $PhoneSerial logcat -c

Write-Host "[2/3] Reiniciando somente o processo do aplicativo..." -ForegroundColor Yellow
& $Adb -s $PhoneSerial shell am force-stop $PackageName | Out-Null
Start-Sleep -Seconds 1
& $Adb -s $PhoneSerial shell am start -n "$PackageName/.MainActivity" | Out-Null

Write-Host "[3/3] Aguardando App Check inicializar..." -ForegroundColor Yellow
Start-Sleep -Seconds 4

$Matches = @(
    & $Adb -s $PhoneSerial logcat -d |
        Select-String -Pattern "DebugAppCheckProvider|debug secret|AppCheck"
)

Write-Host ""
if ($Matches.Count -eq 0) {
    Write-Host "[ATENCAO] Nenhuma linha do App Check foi encontrada." -ForegroundColor DarkYellow
    Write-Host "Confirme que o APK debug mais recente esta instalado e tente novamente."
    exit 2
}

Write-Host "Linhas encontradas:" -ForegroundColor Green
$Matches | ForEach-Object { Write-Host $_.Line }

Write-Host ""
Write-Host "Procure a linha que informa o 'debug secret'." -ForegroundColor Cyan
Write-Host "Cadastre esse valor em Firebase > App Check > Apps > Manage debug tokens." -ForegroundColor Cyan
Write-Host "NAO compartilhe esse token." -ForegroundColor Yellow
