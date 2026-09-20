# Perfil de testes Android - computador do projeto

## Hardware de referência

Configuração informada para o computador de desenvolvimento:

- Windows 10 Pro 64 bits, build 19045;
- Intel Core i5-3470, 4 núcleos / 4 threads;
- 7,98 GB de RAM;
- SSD Kingston SA400S37 480 GB;
- VT-x/EPT suportados pelo processador;
- virtualização Intel VT-x/EPT habilitada no firmware/BIOS e validada no Windows.

## Estratégia

O Android Emulator não é obrigatório para cada alteração.

Prioridade de validação:

1. testes unitários no Windows;
2. compilação do APK de debug;
3. GitHub Actions;
4. Compose Preview quando aplicável;
5. Android Emulator apenas para interface e fluxos que precisem do Android completo;
6. aparelho físico para câmera/QR antes de uma versão de uso diário.

## Perfil AVD Lite

Depois que a virtualização estiver habilitada no BIOS:

- Nome sugerido: LeitorCupons_API34_Lite;
- Android 14 / API 34;
- imagem AOSP x86_64, sem Google Play;
- RAM: 1536 MB;
- CPU: 2 cores; reduzir para 1 se necessário;
- resolução: aproximadamente 720 x 1280;
- gráficos: Automatic;
- Quick Boot ativado;
- câmera desativada quando não estiver sendo testada.

O projeto pode usar compileSdk = 36 e targetSdk = 36 enquanto o dispositivo de teste executa API 34.

## Virtualização

Antes de criar o AVD, habilite no BIOS/UEFI a opção normalmente chamada Intel Virtualization Technology, Intel VT-x ou Virtualization Technology.

Depois de reiniciar o Windows, valide com:

```powershell
Get-CimInstance Win32_Processor |
    Format-List Name,
        VirtualizationFirmwareEnabled,
        VMMonitorModeExtensions,
        SecondLevelAddressTranslationExtensions
```

O valor esperado é:

```text
VirtualizationFirmwareEnabled : True
```

No Windows, o caminho preferencial para aceleração do Android Emulator é a Windows Hypervisor Platform (WHPX).

## Fluxo automatizado

1. Abra o PowerShell como Administrador e execute `scripts/habilitar-whpx.ps1`.
2. Reinicie o Windows se o script solicitar.
3. Em um PowerShell normal, execute `scripts/instalar-emulador-lite.ps1`.
4. Depois execute `scripts/executar-app-emulador.ps1`.

O perfil usa Android 14 / API 34 com imagem AOSP `x86_64`, 1536 MB de RAM, 2 CPUs, resolução 720 x 1280 e câmeras desativadas nesta fase.

A câmera será habilitada posteriormente quando iniciarmos os testes reais de QR Code/NFC-e.
