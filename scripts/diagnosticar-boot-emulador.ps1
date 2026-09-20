param()

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $ProjectRoot

$Sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$Emulator = Join-Path $Sdk "emulator\emulator.exe"
$Adb = Join-Path $Sdk "platform-tools\adb.exe"
$AvdName = "LeitorCupons_API34_Lite"
$PackageName = "br.com.leitorcuponsfinancas"
$Apk = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"

$ReportDir = Join-Path $ProjectRoot "data\runtime\reports"
$StdOutLog = Join-Path $ReportDir "emulator_safe_boot_stdout.log"
$StdErrLog = Join-Path $ReportDir "emulator_safe_boot_stderr.log"

New-Item -ItemType Directory -Path $ReportDir -Force | Out-Null
Remove-Item $StdOutLog, $StdErrLog -Force -ErrorAction SilentlyContinue

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " LEITOR CUPONS FINANCAS - BOOT SEGURO DO EMULADOR" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

foreach ($Required in @($Emulator, $Adb, $Apk)) {
    if (-not (Test-Path $Required)) {
        throw "Arquivo necessario nao encontrado: $Required"
    }
}

Write-Host ""
Write-Host "[1/5] Encerrando instancias residuais..." -ForegroundColor Yellow

Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
    Where-Object {
        ($_.Name -eq "emulator.exe" -or $_.Name -like "qemu-system-*.exe") -and
        $_.CommandLine -match [regex]::Escape($AvdName)
    } |
    ForEach-Object {
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    }

& $Adb kill-server 2>$null | Out-Null
Start-Sleep -Seconds 2
& $Adb start-server | Out-Null

Write-Host ""
Write-Host "[2/5] Iniciando em modo grafico seguro..." -ForegroundColor Yellow
Write-Host "GPU: software | Vulkan: desativado | CPU: 1 | snapshots: desativados" -ForegroundColor DarkYellow

$Arguments = @(
    "@$AvdName",
    "-wipe-data",
    "-no-snapshot",
    "-no-audio",
    "-no-boot-anim",
    "-memory", "1536",
    "-cores", "1",
    "-gpu", "software",
    "-feature", "-Vulkan",
    "-verbose",
    "-show-kernel"
)

$Process = Start-Process -FilePath $Emulator `
    -ArgumentList $Arguments `
    -RedirectStandardOutput $StdOutLog `
    -RedirectStandardError $StdErrLog `
    -PassThru

Write-Host "PID do Emulator: $($Process.Id)"
Write-Host "STDOUT: $StdOutLog"
Write-Host "STDERR: $StdErrLog"

Write-Host ""
Write-Host "[3/5] Aguardando ADB e boot do Android..." -ForegroundColor Yellow

$Serial = $null
$Ready = $false

for ($Attempt = 1; $Attempt -le 180; $Attempt++) {
    Start-Sleep -Seconds 2

    $Process.Refresh()
    if ($Process.HasExited) {
        Write-Host "[ERRO] Emulator encerrou durante o boot. ExitCode: $($Process.ExitCode)" -ForegroundColor Red
        Write-Host ""
        Write-Host "Ultimas linhas do log:" -ForegroundColor Yellow
        if (Test-Path $StdErrLog) {
            Get-Content $StdErrLog -Tail 120
        }
        if (Test-Path $StdOutLog) {
            Get-Content $StdOutLog -Tail 120
        }
        throw "Falha no boot seguro do Android Emulator."
    }

    $Devices = (& $Adb devices | Out-String)

    if ($Devices -match "(?m)^(emulator-\d+)\s+device\b") {
        $Serial = $Matches[1]
        $Boot = (& $Adb -s $Serial shell getprop sys.boot_completed 2>$null | Out-String).Trim()

        if ($Boot -eq "1") {
            $Ready = $true
            break
        }
    }

    if (($Attempt % 15) -eq 0) {
        $Status = if ($Devices -match "emulator-\d+\s+offline") { "ADB offline" } else { "aguardando ADB" }
        Write-Host "Tentativa $Attempt/180 - $Status" -ForegroundColor DarkYellow
    }
}

if (-not $Ready -or -not $Serial) {
    Write-Host ""
    Write-Host "Ultimas linhas do log:" -ForegroundColor Yellow
    if (Test-Path $StdErrLog) {
        Get-Content $StdErrLog -Tail 120
    }
    if (Test-Path $StdOutLog) {
        Get-Content $StdOutLog -Tail 120
    }
    throw "O Android nao concluiu o boot no modo seguro."
}

Write-Host "[OK] Android iniciou em modo seguro: $Serial" -ForegroundColor Green

Write-Host ""
Write-Host "[4/5] Instalando APK..." -ForegroundColor Yellow
& $Adb -s $Serial install -r $Apk
if ($LASTEXITCODE -ne 0) {
    throw "Falha ao instalar o APK."
}

Write-Host ""
Write-Host "[5/5] Abrindo Leitor Cupons Financas..." -ForegroundColor Yellow
& $Adb -s $Serial shell am force-stop $PackageName
& $Adb -s $Serial shell am start -n "$PackageName/.MainActivity"

if ($LASTEXITCODE -ne 0) {
    throw "APK instalado, mas nao foi possivel iniciar o app."
}

Write-Host ""
Write-Host "[OK] App aberto no Emulator em modo grafico seguro." -ForegroundColor Green
Write-Host "Logs desta inicializacao:"
Write-Host $StdOutLog
Write-Host $StdErrLog
