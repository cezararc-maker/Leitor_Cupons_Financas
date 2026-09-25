# Estratégia de testes

## Padrão oficial

A validação funcional e visual do Leitor Cupons Finanças será feita **no aparelho Android físico**.

O Android Emulator deixa de fazer parte do fluxo padrão porque apresentou desempenho inadequado no computador de desenvolvimento. Ele só será utilizado futuramente quando houver solicitação explícita para um teste que realmente precise do ambiente virtual.

O aparelho físico atual de referência é o Moto G15 usado durante o desenvolvimento.

## 1. Testes automatizados

Continuam sendo executados sem Emulator para validar regras e impedir regressões:

- normalização de produtos;
- categorização;
- cálculos financeiros;
- quantidade, preço unitário, desconto e total;
- pagamentos e parcelamento;
- detecção de duplicidade;
- parser de respostas NFC-e salvas como fixtures;
- regras do cadastro manual;
- taxonomia;
- validações de integridade financeira.

Os testes podem rodar pelo Gradle no computador e no GitHub Actions. Eles não exigem abrir o Android Emulator.

## 2. Aparelho Android físico

É o ambiente padrão para validação de:

- telas e layout;
- navegação;
- gestos;
- banco local;
- cadastro e edição;
- câmera;
- leitura de QR;
- leitura por foto;
- permissões;
- consulta à internet;
- NFC-e;
- OCR/IA;
- desempenho real;
- diferentes fluxos de revisão;
- atualização preservando dados.

A instalação deve usar:

```powershell
adb -s <serial> install -r app-debug.apk
```

Nunca usar `uninstall` ou `pm clear` em uma atualização normal.

Para abrir o aplicativo:

```powershell
adb -s <serial> shell am force-stop br.com.leitorcuponsfinancas
adb -s <serial> shell am start -n br.com.leitorcuponsfinancas/.MainActivity
```

Não usar `monkey` apenas para iniciar o app.

## 3. Script padrão de atualização

O script oficial para testes passa a ser:

```powershell
.\scripts\atualizar-app-celular.ps1
```

Ele:

1. verifica alterações locais;
2. atualiza a branch oficial;
3. confirma o celular conectado;
4. executa testes unitários;
5. gera o APK;
6. instala com `adb install -r`;
7. exige retorno `Success`;
8. abre o aplicativo no celular.

Ele **não inicia nem utiliza Emulator**.

Em computadores com pouca memória/CPU, pode ser usado:

```powershell
.\scripts\atualizar-app-celular.ps1 -SafeBuild
```

para limitar o Gradle a um worker e não manter daemon.

## 4. Fixtures de NFC-e

Para evitar depender da disponibilidade da SEFAZ em todos os testes, respostas sanitizadas de exemplo serão mantidas em arquivos de teste.

## 5. Integração SEFAZ-MS

Testes controlados validarão a consulta pública real sem transformar a suíte automatizada em carga sobre o serviço da SEFAZ.

## 6. Android Emulator — somente sob solicitação

O projeto mantém documentação e scripts relacionados ao Emulator apenas para eventual necessidade futura.

O Emulator:

- não será iniciado automaticamente;
- não fará parte dos scripts padrão;
- não será requisito para liberar uma melhoria;
- só será usado quando solicitado explicitamente.
