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
- CPU: 1 core;
- resolução: aproximadamente 720 x 1280;
- gráficos: SwiftShader;
- Vulkan desativado;
- snapshots/Quick Boot desativados para estabilidade;
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
4. Neste computador legado, execute `scripts/instalar-emulador-compat-36.3.10.ps1` para fixar a versão validada.
5. Depois execute `scripts/executar-app-emulador.ps1`.

O perfil validado neste computador usa Android Emulator 36.3.10, Android 14 / API 34 com imagem AOSP `x86_64`, 1536 MB de RAM, 1 CPU, resolução 720 x 1280, SwiftShader, Vulkan desativado e câmeras desativadas nesta fase.

A câmera será habilitada posteriormente quando iniciarmos os testes reais de QR Code/NFC-e.


## Recuperacao do AVD

Se o emulador abrir mas não aparecer no ADB ou não concluir o boot, execute:

```powershell
.\scripts\executar-app-emulador.ps1 -ResetAvd
```

Esse modo encerra processos residuais, reinicia o ADB, executa um cold boot sem snapshots e usa `-wipe-data` para restaurar somente os dados do dispositivo virtual. Os logs de inicialização ficam em `data/runtime/reports/emulator_stdout.log` e `data/runtime/reports/emulator_stderr.log`.


## Compatibilidade com PC legado

O computador de referência do projeto possui Intel Core i5-3470 e 8 GB de RAM, abaixo dos requisitos mínimos atuais publicados pelo Google para Windows com Android Studio + Emulator.

Se o Android Emulator 37.1.11 encerrar durante o boot mesmo com SwiftShader e Vulkan desativado, use o teste controlado com Android Emulator 36.3.10 Stable:

```powershell
.\scripts\instalar-emulador-compat-36.3.10.ps1
.\scripts\diagnosticar-boot-emulador.ps1
```

O script baixa o pacote oficial do Android Emulator Archive, valida o SHA-256, preserva a versão 37.1.11 em `%LocalAppData%\Android\Sdk\emulator-37.1.11-backup` e instala temporariamente a 36.3.10 no diretório padrão do SDK.

Não execute novamente o instalador do Emulator antes do teste de compatibilidade, pois o SDK Manager pode atualizar o componente para a versão mais recente.


## Resultado validado

Em 20/09/2026, o ambiente foi validado com sucesso neste computador usando:

- Android Emulator 36.3.10;
- WHPX ativo;
- Android 14 / API 34 x86_64;
- SwiftShader;
- Vulkan desativado;
- 1 CPU;
- cold boot sem snapshots.

O Android completou o boot, o APK foi instalado via ADB com `Success` e o app `br.com.leitorcuponsfinancas/.MainActivity` abriu corretamente.

Para uso diário, execute:

```powershell
.\scripts\executar-app-emulador.ps1
```

O modo diário não usa `-wipe-data`, portanto os dados persistidos pelo Room devem permanecer entre as execuções. Use `-ResetAvd` apenas para recuperação do dispositivo virtual.


## Teclado físico

O AVD do projeto usa `hw.keyboard=yes`, permitindo digitação com o teclado físico do computador. O script `scripts/executar-app-emulador.ps1` reaplica essa configuração antes de iniciar o AVD.

Se o Emulator já estiver aberto quando a configuração for alterada, feche-o e abra novamente para que a mudança seja aplicada.

## Persistência local validada

Foi validado que um produto cadastrado permanece salvo após fechar e reabrir o Android Emulator. Isso confirma, no fluxo atual do MVP, a persistência local por Room entre execuções normais do AVD.

Não use `-ResetAvd` no uso diário, pois esse modo executa `-wipe-data` e apaga os dados do dispositivo virtual.
