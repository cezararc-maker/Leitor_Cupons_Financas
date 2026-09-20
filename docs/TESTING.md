# Estratégia de testes

## 1. Testes unitários no computador

Executados sem celular para validar:
- normalização de produtos;
- categorização;
- cálculos de quantidade, preço unitário e total;
- detecção de duplicidade;
- parser de respostas NFC-e salvas como fixtures;
- regras do cadastro manual.

## 2. Android Emulator

O aplicativo será executado em um dispositivo Android virtual no Windows.

Será possível validar:
- telas;
- navegação;
- banco local;
- cadastro/edição/exclusão de produtos;
- permissões;
- consulta à internet;
- fluxo da NFC-e;
- leitura de QR usando imagem inserida na câmera virtual.

## 3. Fixtures de NFC-e

Para evitar depender da disponibilidade da SEFAZ em todos os testes, respostas sanitizadas de exemplo serão mantidas em arquivos de teste.

## 4. Testes de integração SEFAZ-MS

Testes controlados validarão a consulta pública real sem transformar a suíte automatizada em carga sobre o serviço da SEFAZ.

## 5. Dispositivo físico

Antes de uma versão de uso diário, o APK será testado em aparelho Android real para validar:
- foco e desempenho da câmera;
- leitura de QR em cupom térmico;
- permissões;
- comportamento com internet móvel/Wi-Fi;
- diferentes tamanhos de tela.
