# Ambiente mínimo no Windows

Para o computador de desenvolvimento atual, o projeto prioriza um ambiente leve e não exige Android Studio nesta fase.

## Componentes

- Microsoft OpenJDK 17;
- Android SDK Command-Line Tools;
- Android SDK Platform 36;
- Android Build Tools 35.0.0;
- Android Platform Tools;
- Gradle Wrapper do próprio projeto.

O Android Studio e o Android Emulator ficam opcionais para uma fase posterior.

## Instalação manual das Command-Line Tools

Baixe no site oficial Android Developers o pacote **Command line tools only** para Windows.

Estrutura esperada depois de extrair:

`%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat`

A pasta `latest` deve conter diretamente as pastas `bin` e `lib`, além dos arquivos do pacote.

Depois execute na raiz do projeto:

```powershell
Set-ExecutionPolicy -Scope Process Bypass -Force
.\scripts\instalar-ambiente-minimo.ps1
```

O script usa o `sdkmanager.bat` para aceitar licenças e instalar:

- `platform-tools`
- `platforms;android-36`
- `build-tools;35.0.0`

O SDK fica em:

`%LOCALAPPDATA%\Android\Sdk`

## Vantagem

Esse fluxo permite continuar usando VS Code, PowerShell e GitHub, evitando manter o Android Studio aberto em uma máquina com 8 GB de RAM.

## Emulador

O emulador continua opcional. Antes de usá-lo, a virtualização Intel VT-x deve ser habilitada no BIOS/UEFI. Consulte `docs/EMULATOR_LITE.md`.


## Gradle Wrapper local

Nesta fase, `gradlew`, `gradlew.bat` e `gradle/wrapper` são gerados automaticamente por `scripts/bootstrap-windows.ps1` e permanecem locais. Eles são ignorados pelo Git para evitar arquivos gerados aparecendo como alterações do projeto.
