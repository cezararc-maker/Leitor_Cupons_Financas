param()

$ErrorActionPreference = "Stop"

$Sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$EmulatorDir = Join-Path $Sdk "emulator"
$BackupDir = Join-Path $Sdk "emulator-37.1.11-backup"
$TempRoot = Join-Path $env:TEMP "LeitorCupons_Emulator_36_3_10"
$Zip = Join-Path $TempRoot "emulator-windows_x64-14472402.zip"
$ExtractDir = Join-Path $TempRoot "extract"
$Url = "https://edgedl.me.gvt1.com/edgedl/android/repository/emulator-windows_x64-14472402.zip"
$ExpectedSha = "FBD5E54C868F66DE78FA3A8B811DAAA58262CADDF8920271A63F77386CCE21CA"
$TargetVersion = "36.3.10"

$Adb = Join-Path $Sdk "platform-tools\adb.exe"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " LEITOR CUPONS FINANCAS - EMULATOR COMPAT 36.3.10" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

if (-not (Test-Path $EmulatorDir)) {
    throw "Diretorio atual do Emulator nao encontrado: $EmulatorDir"
}

if (Test-Path $Adb) {
    & $Adb kill-server 2>$null | Out-Null
}

Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
    Where-Object {
        $_.Name -eq "emulator.exe" -or $_.Name -like "qemu-system-*.exe"
    } |
    ForEach-Object {
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    }

Start-Sleep -Seconds 2

Write-Host ""
Write-Host "[1/6] Preparando download oficial..." -ForegroundColor Yellow

New-Item -ItemType Directory -Path $TempRoot -Force | Out-Null
Remove-Item $Zip -Force -ErrorAction SilentlyContinue
Remove-Item $ExtractDir -Recurse -Force -ErrorAction SilentlyContinue

Write-Host "Fonte oficial Android Emulator Archive:"
Write-Host $Url
Write-Host "Tamanho aproximado: 447 MB"

try {
    Import-Module BitsTransfer -ErrorAction Stop
    Start-BitsTransfer -Source $Url -Destination $Zip -DisplayName "Android Emulator 36.3.10"
} catch {
    Write-Host "[INFO] BITS indisponivel; usando Invoke-WebRequest..." -ForegroundColor DarkYellow
    Invoke-WebRequest -Uri $Url -OutFile $Zip -UseBasicParsing
}

if (-not (Test-Path $Zip)) {
    throw "Download do Emulator nao foi concluido."
}

Write-Host ""
Write-Host "[2/6] Validando SHA-256..." -ForegroundColor Yellow

$ActualSha = (Get-FileHash -Path $Zip -Algorithm SHA256).Hash.ToUpperInvariant()

Write-Host "Esperado: $ExpectedSha"
Write-Host "Obtido:   $ActualSha"

if ($ActualSha -ne $ExpectedSha) {
    throw "SHA-256 divergente. O arquivo baixado nao sera instalado."
}

Write-Host "[OK] Pacote oficial validado." -ForegroundColor Green

Write-Host ""
Write-Host "[3/6] Extraindo Emulator $TargetVersion..." -ForegroundColor Yellow

Expand-Archive -Path $Zip -DestinationPath $ExtractDir -Force

$ExtractedEmulator = Join-Path $ExtractDir "emulator"
if (-not (Test-Path (Join-Path $ExtractedEmulator "emulator.exe"))) {
    throw "Estrutura inesperada no pacote baixado."
}

Write-Host ""
Write-Host "[4/6] Fazendo backup do Emulator atual..." -ForegroundColor Yellow

if (Test-Path $BackupDir) {
    Remove-Item $BackupDir -Recurse -Force
}

Move-Item -Path $EmulatorDir -Destination $BackupDir
Write-Host "[OK] Backup: $BackupDir" -ForegroundColor Green

try {
    Move-Item -Path $ExtractedEmulator -Destination $EmulatorDir

    $OriginalPackageXml = Join-Path $BackupDir "package.xml"
    $NewPackageXml = Join-Path $EmulatorDir "package.xml"

    if (Test-Path $OriginalPackageXml) {
        Copy-Item $OriginalPackageXml $NewPackageXml -Force

        $PackageText = Get-Content $NewPackageXml -Raw
        $RevisionPattern = '<revision>\s*<major>\d+</major>\s*<minor>\d+</minor>\s*<micro>\d+</micro>\s*</revision>'
        $RevisionReplacement = "<revision><major>36</major><minor>3</minor><micro>10</micro></revision>"

        $PackageText = [regex]::Replace(
            $PackageText,
            $RevisionPattern,
            $RevisionReplacement,
            [System.Text.RegularExpressions.RegexOptions]::Singleline
        )

        Set-Content -Path $NewPackageXml -Value $PackageText -Encoding UTF8
    }

    Write-Host ""
    Write-Host "[5/6] Validando versao instalada..." -ForegroundColor Yellow

    $EmulatorExe = Join-Path $EmulatorDir "emulator.exe"
    & $EmulatorExe -version

    if ($LASTEXITCODE -ne 0) {
        throw "Emulator $TargetVersion nao iniciou corretamente."
    }

    Write-Host ""
    Write-Host "[6/6] Concluido." -ForegroundColor Yellow
    Write-Host "[OK] Android Emulator $TargetVersion instalado para teste de compatibilidade." -ForegroundColor Green
    Write-Host "[OK] Emulator 37.1.11 preservado em: $BackupDir" -ForegroundColor Green
    Write-Host ""
    Write-Host "Nao execute o instalador do Emulator novamente antes do teste, pois ele pode atualizar para a versao mais recente." -ForegroundColor DarkYellow
    Write-Host "Proximo comando:"
    Write-Host ".\scripts\diagnosticar-boot-emulador.ps1" -ForegroundColor Cyan
}
catch {
    Write-Host ""
    Write-Host "[ERRO] Falha durante a troca de versao. Restaurando Emulator anterior..." -ForegroundColor Red

    if (Test-Path $EmulatorDir) {
        Remove-Item $EmulatorDir -Recurse -Force -ErrorAction SilentlyContinue
    }

    if (Test-Path $BackupDir) {
        Move-Item -Path $BackupDir -Destination $EmulatorDir -Force
    }

    throw
}
