# Comprovante de cartão — V1

## Objetivo

Registrar compras a partir de comprovantes de maquininhas de cartão quando não há NFC-e disponível no momento da captura.

O comprovante informa principalmente o pagamento. Por isso, os produtos não são inventados pelo OCR: o usuário precisa informá-los antes de salvar.

## Fluxo

No botão **+** existe a opção **Comprovante de cartão**.

1. Tirar uma foto ou escolher imagem/PDF.
2. OCR identifica, quando presente:
   - estabelecimento;
   - CNPJ;
   - data e hora;
   - valor total;
   - débito ou crédito;
   - quantidade de parcelas quando impressa;
   - bandeira;
   - últimos quatro dígitos mascarados.
3. Usuário revisa os dados.
4. Em crédito sem informação confiável de parcelamento, o usuário precisa confirmar **À vista** ou **Parcelado**.
5. Usuário informa pelo menos um produto comprado.
6. O app compara a soma dos produtos com o valor do comprovante.
7. Diferença positiva exige correção ou confirmação explícita de **valor não identificado**.
8. Quando confirmada, a diferença é preservada como um item revisável chamado **Valor não identificado**, para que o total não seja perdido silenciosamente.
9. Soma de produtos acima do total do comprovante é bloqueada.
10. A compra é salva no Histórico com `sourceType = CARD_RECEIPT`.

## Produtos

Cada item exige:

- descrição/produto;
- quantidade;
- valor total.

Produtos Mestres existentes são sugeridos durante a digitação. Se o usuário selecionar uma sugestão, o vínculo é salvo. Uma descrição digitada sem vínculo permanece disponível para revisão posterior.

## Pagamento

O pagamento é salvo em `payment_allocations`.

Métodos da V1:

- `DEBIT`;
- `CREDIT`;
- `OTHER`.

Para crédito:

- à vista → `installmentCount = 1`;
- parcelado → quantidade de parcelas obrigatória;
- se o comprovante disser apenas “crédito”, o app não presume à vista.

## Privacidade

O app não deve armazenar número completo do cartão.

Somente:

- bandeira, quando disponível;
- últimos quatro dígitos de um número já mascarado no comprovante.

Código de autorização, AID, identificadores EMV e PAN completo não são necessários para a análise de gastos e não fazem parte da V1.

## Banco local

A versão do Room foi elevada para 8 com migração `7 → 8`, criando:

```
payment_allocations
```

O backup restaurável do banco local inclui naturalmente essa nova tabela.

## Validação antes de distribuir

1. Compilação e testes no GitHub Actions.
2. Atualização do Moto G15 por ADB somente quando o usuário decidir testar.
3. Testar foto do comprovante real.
4. Conferir OCR dos campos.
5. Testar débito.
6. Testar crédito à vista.
7. Testar crédito parcelado.
8. Testar soma de produtos igual ao total.
9. Testar diferença com “valor não identificado”.
10. Conferir registro no Histórico e persistência após fechar/abrir o app.
11. Só depois liberar aos testadores.


## Exclusão e correção

- itens podem ser corrigidos no Histórico;
- comprovantes de cartão podem ser excluídos porque são lançamentos locais;
- a exclusão pelo Histórico remove a compra inteira, todos os itens e o pagamento daquele comprovante, evitando deixar totais inconsistentes;
- NFC-e continua com as regras próprias de preservação.
