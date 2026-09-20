param(
    [switch]$ResetAvd
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $ProjectRoot

$Sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$Emulator = Join-Path $Sdk "emulator\emulator.exe"
$Adb = Join-Path $Sdk "platform-tools\adb.exe"
$AvdName = "LeitorCupons_API34_Lite"
$PackageName = "br.com.leitorcuponsfinancas"
$Apk = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"
$AvdConfig = Join-Path $env:USERPROFILE ".android\avd\$AvdName.avd\config.ini"

$ReportDir = Join-Path $ProjectRoot "data\runtime\reports"
$StdOutLog = Join-Path $ReportDir "emulator_stdout.log"
$StdErrLog = Join-Path $ReportDir "emulator_stderr.log"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " LEITOR CUPONS FINANCAS - EXECUTAR NO EMULADOR" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

foreach ($Required in @($Emulator, $Adb, $Apk)) {
    if (-not (Test-Path $Required)) {
        throw "Arquivo necessario nao encontrado: $Required"
    }
}

New-Item -ItemType Directory -Path $ReportDir -Force | Out-Null

function Enable-PhysicalKeyboard {
    if (-not (Test-Path $AvdConfig)) {
        Write-Host "[INFO] config.ini do AVD ainda nao encontrado; teclado sera configurado quando o AVD existir." -ForegroundColor DarkYellow
        return
    }

    $Lines = Get-Content $AvdConfig
    $Pattern = "^hw\.keyboard="

    if ($Lines -match $Pattern) {
        $Lines = $Lines | ForEach-Object {
            if ($_ -match $Pattern) { "hw.keyboard=yes" } else { $_ }
        }
    } else {
        $Lines += "hw.keyboard=yes"
    }

    Set-Content -Path $AvdConfig -Value $Lines -Encoding ASCII
}

function Get-ProjectAvdProcesses {
    @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
        Where-Object {
            ($_.Name -eq "emulator.exe" -or $_.Name -like "qemu-system-*.exe") -and
            $_.CommandLine -match [regex]::Escape($AvdName)
        })
}

function Stop-ProjectAvd {
    $Processes = Get-ProjectAvdProcesses

    if ($Processes.Count -gt 0) {
        Write-Host "[INFO] Encerrando processo(s) do AVD $AvdName..." -ForegroundColor DarkYellow
        $Processes | ForEach-Object {
            Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
        }
        Start-Sleep -Seconds 3
    }
}

function Restart-Adb {
    try {
        & $Adb kill-server 2>$null | Out-Null
    } catch {
        Write-Host "[INFO] ADB ja estava parado." -ForegroundColor DarkYellow
    }

    Start-Sleep -Seconds 1

    try {
        & $Adb start-server | Out-Null
    } catch {
        throw "Nao foi possivel iniciar o ADB server."
    }
}

function Start-ProjectAvd {
    param(
        [switch]$WipeData
    )

    Remove-Item $StdOutLog, $StdErrLog -Force -ErrorAction SilentlyContinue

    $Arguments = @(
        "@$AvdName",
        "-gpu", "swiftshader",
        "-feature", "-Vulkan",
        "-no-audio",
        "-no-boot-anim",
        "-no-snapshot",
        "-memory", "1536",
        "-cores", "1"
    )

    if ($WipeData) {
        $Arguments += "-wipe-data"
        Write-Host "[INFO] Iniciando AVD com reset de fabrica e cold boot..." -ForegroundColor DarkYellow
    } else {
        Write-Host "Iniciando $AvdName em modo compativel (SwiftShader, Vulkan off, 1 CPU)..."
    }

    Start-Process -FilePath $Emulator `
        -ArgumentList $Arguments `
        -RedirectStandardOutput $StdOutLog `
        -RedirectStandardError $StdErrLog | Out-Null
}

function Show-EmulatorDiagnostics {
    Write-Host ""
    Write-Host "Diagnostico do ADB:" -ForegroundColor Yellow
    & $Adb devices -l

    Write-Host ""
    Write-Host "Processos do AVD:" -ForegroundColor Yellow
    Get-ProjectAvdProcesses |
        Select-Object ProcessId, Name, CommandLine |
        Format-List

    if (Test-Path $StdErrLog) {
        Write-Host ""
        Write-Host "Ultimas linhas do log do emulador:" -ForegroundColor Yellow
        Get-Content $StdErrLog -Tail 40
    }

    Write-Host ""
    Write-Host "Logs completos:" -ForegroundColor Yellow
    Write-Host $StdOutLog
    Write-Host $StdErrLog
}

Write-Host ""
Write-Host "[1/4] Preparando emulador..." -ForegroundColor Yellow

Enable-PhysicalKeyboard
Restart-Adb

if ($ResetAvd) {
    Stop-ProjectAvd
    Start-ProjectAvd -WipeData
} else {
    $Devices = (& $Adb devices | Out-String)

    if ($Devices -notmatch "(?m)^emulator-\d+\s+device\b") {
        $RunningAvd = Get-ProjectAvdProcesses

        if ($RunningAvd.Count -gt 0) {
            Write-Host "[INFO] Processo do AVD encontrado sem conexao ADB. Aguardando 10 segundos..." -ForegroundColor DarkYellow
            Start-Sleep -Seconds 10

            $DevicesAfterWait = (& $Adb devices | Out-String)

            if ($DevicesAfterWait -notmatch "(?m)^emulator-\d+\s+device\b") {
                Write-Host "[INFO] Processo residual ou boot travado detectado. Reiniciando em cold boot..." -ForegroundColor DarkYellow
                Stop-ProjectAvd
                Restart-Adb
                Start-ProjectAvd
            }
        } else {
            Start-ProjectAvd
        }
    }
}

Write-Host ""
Write-Host "[2/4] Aguardando Android concluir a inicializacao..." -ForegroundColor Yellow

$Ready = $false
$Serial = $null

for ($Attempt = 1; $Attempt -le 180; $Attempt++) {
    Start-Sleep -Seconds 2

    $DevicesText = (& $Adb devices | Out-String)

    if ($DevicesText -match "(?m)^(emulator-\d+)\s+device\b") {
        $Serial = $Matches[1]

        $Boot = (& $Adb -s $Serial shell getprop sys.boot_completed 2>$null | Out-String).Trim()

        if ($Boot -eq "1") {
            $Ready = $true
            break
        }
    }

    if ($Attempt -ge 10 -and (Get-ProjectAvdProcesses).Count -eq 0) {
        Write-Host "[ERRO] O processo do emulador encerrou durante o boot." -ForegroundColor Red
        Show-EmulatorDiagnostics
        throw "O Android Emulator encerrou antes de concluir a inicializacao."
    }

    if (($Attempt % 15) -eq 0) {
        $Status = if ($DevicesText -match "emulator-\d+\s+offline") { "ADB offline" } else { "aguardando ADB" }
        Write-Host "Aguardando emulador... tentativa $Attempt/180 ($Status)" -ForegroundColor DarkYellow
    }
}

if (-not $Ready -or -not $Serial) {
    Show-EmulatorDiagnostics

    if (-not $ResetAvd) {
        Write-Host ""
        Write-Host "[RECUPERACAO] Execute novamente com -ResetAvd para restaurar o AVD." -ForegroundColor Yellow
        Write-Host ".\scripts\executar-app-emulador.ps1 -ResetAvd" -ForegroundColor Cyan
    }

    throw "O Android nao concluiu a inicializacao dentro do limite de verificacao."
}

Write-Host "[OK] Android iniciado: $Serial" -ForegroundColor Green

Write-Host ""
Write-Host "[3/4] Instalando APK..." -ForegroundColor Yellow
& $Adb -s $Serial install -r $Apk
if ($LASTEXITCODE -ne 0) {
    throw "Falha ao instalar o APK no emulador."
}

Write-Host ""
Write-Host "[4/4] Abrindo Leitor Cupons Financas..." -ForegroundColor Yellow
& $Adb -s $Serial shell am force-stop $PackageName
& $Adb -s $Serial shell am start -n "$PackageName/.MainActivity"

if ($LASTEXITCODE -ne 0) {
    throw "APK instalado, mas o app nao pode ser iniciado automaticamente."
}

Write-Host ""
Write-Host "[OK] Leitor Cupons Financas aberto no emulador." -ForegroundColor Green
