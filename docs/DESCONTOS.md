# Descontos por item e por compra

## Objetivo

Preservar a diferença entre o preço bruto e o preço efetivamente pago, tanto em NFC-e/NF-e quanto em lançamentos manuais, OCR e IA.

## Origem fiscal

No leiaute da NF-e/NFC-e existe campo de desconto por item (`vDesc`) e também totalização de desconto da nota. O importador deve aproveitar esses valores quando estiverem disponíveis.

## Modelo conceitual

### Item

```
Produto: Café
Quantidade: 2
Valor bruto: R$ 30,00
Desconto:   R$  4,00
Valor líquido: R$ 26,00
```

### Compra

```
Subtotal bruto:    R$ 180,00
Descontos itens:   R$  12,00
Desconto global:   R$   5,00
Total líquido:     R$ 163,00
```

Quando existir desconto global sem vínculo seguro com os itens, ele deve permanecer no nível da compra. Não fazer rateio arbitrário.

## Cadastro manual

O lançamento manual deve permitir:

- desconto por item;
- desconto total da compra;
- valor bruto;
- valor líquido calculado automaticamente.

A entrada deve continuar usando o padrão monetário do app.

## Análises

Os descontos permitirão:

- total economizado no mês;
- percentual de desconto sobre o valor bruto;
- economia por Produto Mestre;
- economia por categoria;
- economia por estabelecimento;
- produtos com maior desconto absoluto;
- produtos com maior desconto percentual;
- preço bruto médio x preço líquido médio;
- evolução do preço efetivamente pago.

## Comparação de preços

Para comparação histórica de preço do Produto Mestre, o app deve distinguir:

- preço anunciado/bruto, quando conhecido;
- desconto aplicado;
- preço líquido efetivamente pago.

O indicador principal de gasto deve usar o **preço líquido**, mas o preço bruto deve ser mantido para medir economia e promoções.

## OCR e IA

Ao ler fotos, PDFs ou recibos, procurar termos como:

- DESCONTO;
- DESC;
- ECONOMIA;
- CUPOM;
- OFERTA;
- PROMOÇÃO;
- TOTAL DESCONTOS.

Qualquer valor inferido automaticamente deve passar pela revisão do usuário antes de salvar.


## Interface no lançamento manual

O lançamento manual deve ter uma opção visual de ativação de desconto.

Exemplo:

```
○ Desconto
```

Ao marcar:

```
● Desconto

Tipo:
○ Valor por unidade
○ Percentual por unidade
○ Valor total do item

Valor/percentual: [        ]
```

Apenas uma modalidade de desconto do item deve ficar ativa por vez.

### Cálculos

Exemplo por valor unitário:

```
Quantidade: 3
Preço bruto unitário: R$ 10,00
Desconto unitário:    R$  2,00

Bruto:      R$ 30,00
Desconto:   R$  6,00
Líquido:    R$ 24,00
```

Exemplo percentual:

```
Quantidade: 2
Preço bruto unitário: R$ 50,00
Desconto: 10%

Bruto:      R$ 100,00
Desconto:   R$  10,00
Líquido:    R$  90,00
```

O usuário também pode informar desconto global da compra. Esse desconto global não deve ser distribuído automaticamente entre itens sem uma regra segura.

## Edição pelo Histórico

Ao abrir uma compra/NF, o desconto total pode ser revisado no cabeçalho da compra.

Ao abrir um item, o usuário pode revisar:

- preço bruto;
- desconto unitário;
- percentual;
- desconto total do item;
- preço líquido.

O histórico deve preservar os valores originais/importados e registrar a correção do usuário quando houver edição.
