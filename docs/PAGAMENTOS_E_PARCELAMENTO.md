# Formas de pagamento e parcelamento

## Objetivo

Registrar como cada compra foi paga e usar essa informação no Histórico, Dashboard e análises futuras.

## Por que não usar um único campo na compra

Uma compra pode ser paga de forma mista, por exemplo:

```
Total: R$ 180,00

PIX        R$ 80,00
Crédito    R$ 100,00 em 2x
```

Por isso a compra terá uma coleção de pagamentos, e não apenas `paymentMethod = CREDIT`.

## Formas iniciais

- PIX
- Dinheiro
- Débito
- Crédito
- Outros

## Crédito

Quando a forma for Crédito, registrar quando disponível:

- valor pago no crédito;
- à vista ou parcelado;
- quantidade de parcelas;
- futuramente, cartão utilizado;
- futuramente, primeira competência/vencimento, quando confiável.

## Origem do dado

A informação pode vir de:

1. NFC-e/documento fiscal;
2. OCR local;
3. leitura por IA;
4. confirmação manual do usuário.

Quando o documento fiscal trouxer o meio e o valor do pagamento, esses dados devem ser importados automaticamente. O usuário só precisa complementar o que o documento não informa com segurança, especialmente o parcelamento.

A prioridade é preservar o dado original. Se o documento disser apenas `Cartão`, não converter automaticamente para Débito ou Crédito sem evidência.

## Revisão

Na tela de revisão da compra:

```
Forma de pagamento

[ PIX ]
[ Dinheiro ]
[ Débito ]
[ Crédito ]

Crédito:
( ) À vista
( ) Parcelado

Parcelas: [ 3 ]

[ + Adicionar outra forma ]
```

O app deve validar a soma das formas de pagamento contra o total da compra.

Para Crédito, se a nota indicar apenas "Cartão de Crédito" e não trouxer quantidade de parcelas, a revisão deve perguntar:

```
Esta compra foi parcelada?

( ) Não / crédito à vista
( ) Sim

Quantidade de parcelas: [  ]
```

Essa informação será fornecida pelo usuário e marcada como origem `USER`.

## Análises

### Por método

- total pago via PIX;
- total pago em dinheiro;
- total no débito;
- total no crédito;
- participação percentual de cada método.

### Crédito

- total comprado no crédito;
- compras no crédito à vista;
- compras parceladas;
- número médio de parcelas;
- maior parcelamento;
- percentual do consumo que foi parcelado.

### Fluxo futuro

Somente quando houver dados suficientes para uma projeção confiável:

- parcelas ainda a vencer;
- valor comprometido nos próximos meses;
- compromissos por cartão;
- consumo x desembolso.

Exemplo:

```
Consumo de setembro:
R$ 2.400,00

Pago imediatamente:
PIX/Dinheiro/Débito     R$ 1.300,00

Crédito à vista         R$   500,00
Crédito parcelado       R$   600,00
```

## Regra conceitual importante

Uma compra parcelada continua pertencendo integralmente ao mês em que ocorreu para análises de **consumo**.

Exemplo:

```
Compra em 25/09
R$ 900,00 em 3x

Consumo de setembro:
R$ 900,00
```

Já uma futura visão de **fluxo financeiro** poderá distribuir o pagamento:

```
Outubro     R$ 300,00
Novembro    R$ 300,00
Dezembro    R$ 300,00
```

Isso impede que o app confunda "quando comprei" com "quando o dinheiro efetivamente saiu".

## Integração com leitura por foto

A evolução do OCR/IA deve tentar identificar expressões como:

- PIX;
- DINHEIRO;
- CARTÃO;
- DÉBITO;
- CRÉDITO;
- VISA / MASTERCARD / ELO;
- PARCELADO;
- 2x, 3x, 10x etc.

Todo resultado automático continua sujeito à revisão do usuário.
