# Modelo de dados inicial

## Product

Representa um produto conhecido pelo usuário.

- id
- fiscalDescription
- normalizedName
- sector
- category
- subcategory
- unit
- notes
- active
- createdAt
- updatedAt

## Purchase

Representa uma compra.

- id
- source: NFCE | MANUAL
- establishmentName
- documentNumber
- address
- purchasedAt
- grossTotal
- discountTotal
- total
- nfceAccessKey
- nfceUrl
- createdAt

## PurchaseItem

Item efetivamente comprado.

- id
- purchaseId
- productId opcional
- fiscalDescription
- quantity
- unit
- unitPrice
- grossTotalPrice
- discountAmount
- totalPrice
- sector
- category
- subcategory
- needsReview

## Regra importante

O item da compra mantém sua própria classificação histórica. Assim, se a categoria padrão de um produto mudar no futuro, compras antigas não serão alteradas silenciosamente.


## PaymentAllocation

Representa uma parcela do pagamento de uma compra. Uma compra pode possuir mais de um registro, permitindo pagamento misto.

- id
- purchaseId
- method: PIX | CASH | DEBIT | CREDIT | OTHER
- amount
- installmentCount: opcional; para crédito, 1 significa crédito à vista
- instrumentId: opcional e futuro; identifica cartão/conta quando houver cadastro de instrumentos financeiros
- firstDueDate: opcional; usado apenas quando houver informação confiável para projeção de fluxo
- source: NFCE | OCR | AI | USER
- confidence: opcional; utilizado quando o dado for inferido por leitura automática
- createdAt
- updatedAt

### Regras de pagamento

- Importar automaticamente `tPag`/meio de pagamento e `vPag`/valor pago quando esses dados estiverem disponíveis no documento fiscal.
- Não limitar a compra a uma única forma de pagamento.
- A soma dos `PaymentAllocation.amount` deve ser validada contra o total da compra, admitindo diferença controlada para arredondamentos/ajustes.
- Parcelamento pertence ao pagamento em Crédito, não ao Produto Mestre.
- Se o documento informar apenas `Cartão` sem distinguir débito/crédito, o dado deve permanecer pendente para revisão em vez de ser inventado.
- Se o número de parcelas não estiver disponível na NFC-e/recibo, o usuário poderá informar manualmente.
- A ausência de informação de parcelamento nunca deve ser interpretada automaticamente como crédito à vista.
- O histórico deve permitir duas leituras diferentes no futuro:
  - **Consumo**: valor integral na data da compra;
  - **Fluxo financeiro**: parcelas distribuídas por competência, somente quando vencimentos/cartão permitirem cálculo confiável.


## Descontos

Desconto não é uma forma de pagamento e não deve ser misturado com parcelas.

### Na compra

- grossTotal: total antes dos descontos;
- discountTotal: desconto total informado/apurado;
- total: valor líquido final da compra.

### No item

- grossTotalPrice: valor bruto do item;
- discountAmount: desconto atribuído ao item;
- totalPrice: valor líquido efetivo do item.

### Regras

- Importar desconto por item quando o documento fiscal trouxer `vDesc` no item.
- Importar o desconto total quando o documento trouxer a totalização correspondente.
- Preservar o valor bruto e o valor líquido; não substituir um pelo outro.
- No lançamento manual, permitir desconto por item e desconto total.
- Se houver desconto global que não possa ser distribuído com segurança entre os itens, mantê-lo apenas no nível da compra em vez de inventar rateio.
