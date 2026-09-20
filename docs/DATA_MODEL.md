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
