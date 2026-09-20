# Ambiente mínimo no Windows

Para o computador de desenvolvimento atual, o projeto prioriza um ambiente leve e não exige Android Studio nesta fase.

## Componentes

- Microsoft OpenJDK 17;
- Android CLI oficial do Google;
- Android SDK Platform 36;
- Android Build Tools 35.0.0;
- Android Platform Tools;
- Gradle Wrapper do próprio projeto.

O Android Studio e o Android Emulator ficam opcionais para uma fase posterior.

## Instalação

Na raiz do projeto:

```powershell
Set-ExecutionPolicy -Scope Process Bypass -Force
.\scripts\instalar-ambiente-minimo.ps1
```

O script usa `winget` para instalar o JDK e a Android CLI e configura o SDK em:

`%LOCALAPPDATA%\Android\Sdk`

## Vantagem

Esse fluxo permite continuar usando VS Code, PowerShell e GitHub, evitando manter o Android Studio aberto em uma máquina com 8 GB de RAM.

## Emulador

O emulador continua opcional. Antes de usá-lo, a virtualização Intel VT-x deve ser habilitada no BIOS/UEFI. Consulte `docs/EMULATOR_LITE.md`.
