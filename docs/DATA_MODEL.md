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

- Não limitar a compra a uma única forma de pagamento.
- A soma dos `PaymentAllocation.amount` deve ser validada contra o total da compra, admitindo diferença controlada para arredondamentos/ajustes.
- Parcelamento pertence ao pagamento em Crédito, não ao Produto Mestre.
- Se o documento informar apenas `Cartão` sem distinguir débito/crédito, o dado deve permanecer pendente para revisão em vez de ser inventado.
- Se o número de parcelas não estiver disponível na NFC-e/recibo, o usuário poderá informar manualmente.
- O histórico deve permitir duas leituras diferentes no futuro:
  - **Consumo**: valor integral na data da compra;
  - **Fluxo financeiro**: parcelas distribuídas por competência, somente quando vencimentos/cartão permitirem cálculo confiável.
