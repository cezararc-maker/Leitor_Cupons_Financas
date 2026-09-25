# Leitor Cupons Finanças

Aplicativo Android para registrar e classificar gastos a partir de cupons fiscais, com foco inicial em NFC-e emitidas no Mato Grosso do Sul (MS).

## Escopo inicial

- Leitura de QR Code de NFC-e/MS.
- Consulta e interpretação dos dados públicos da NFC-e.
- Cadastro manual de produtos.
- Classificação por setor, categoria e subcategoria.
- Armazenamento local no Android.
- Revisão dos itens antes de salvar uma compra.
- Base preparada para exportação e dashboards em fases posteriores.

## Estado atual

A primeira implementação contém:

- projeto Android em Kotlin + Jetpack Compose;
- persistência local com Room/SQLite;
- tela inicial;
- cadastro, edição e desativação de produtos;
- campos de setor, categoria, subcategoria, unidade e observações;
- normalização básica de nomes;
- testes unitários iniciais;
- script de preparação e testes para Windows;
- workflow de integração contínua no GitHub.

A leitura da NFC-e por QR Code será a próxima etapa funcional.

## Estratégia de desenvolvimento

O GitHub é a fonte oficial do projeto. O desenvolvimento é feito em branches de trabalho, mantendo a branch `main` estável.

Branch atual do MVP:

`feat/android-ms-mvp`

## Preparação no Windows

Pré-requisitos:

- Git;
- Android Studio com Android SDK;
- virtualização habilitada para usar o Android Emulator.

Depois de clonar a branch, execute:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\scripts\bootstrap-windows.ps1
```

O script:

1. usa o JDK incluído no Android Studio quando `JAVA_HOME` não estiver configurado;
2. localiza o Android SDK;
3. cria o `local.properties`;
4. baixa o Gradle 9.6.0 somente se o wrapper ainda não existir;
5. gera o Gradle Wrapper;
6. executa os testes unitários.

## Executar no Android Emulator

Abra a pasta do projeto no Android Studio, aguarde a sincronização do Gradle, crie um Android Virtual Device em **Device Manager** e execute o módulo `app`.

O Android Emulator será nosso ambiente principal de validação antes de instalar APKs em um aparelho físico.

## Testes

O projeto será testado em três níveis:

1. testes unitários de regras, parser e classificação;
2. Android Emulator no computador;
3. dispositivo Android real antes de uma versão de uso diário.

O Android Emulator permite inclusive testar QR Codes usando imagens inseridas na câmera virtual.

## Roadmap e continuidade

O estado atual, decisões de produto e a ordem oficial das próximas etapas estão consolidados em:

- [docs/ROADMAP.md](docs/ROADMAP.md)

Esse arquivo deve ser atualizado sempre que uma etapa importante for concluída ou a prioridade do projeto mudar.

