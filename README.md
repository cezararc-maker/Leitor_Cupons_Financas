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

## Estratégia de desenvolvimento

O GitHub é a fonte oficial do projeto. O desenvolvimento será feito em branches de trabalho, mantendo a branch `main` estável.

## Testes

O projeto será testado em três níveis:

1. testes unitários de regras, parser e classificação;
2. Android Emulator no computador;
3. dispositivo Android real antes de uma versão de uso diário.

O Android Emulator permitirá inclusive testar o leitor de QR Code usando imagens de cupons na câmera virtual.
