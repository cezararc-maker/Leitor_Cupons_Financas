package br.com.leitorcuponsfinancas.data

import br.com.leitorcuponsfinancas.domain.NfceReceipt
import br.com.leitorcuponsfinancas.domain.ProductNormalizer

class ReceiptRepository(
    private val receiptDao: ReceiptDao,
    private val productDao: ProductDao,
) {

    suspend fun save(
        accessKey: String,
        receipt: NfceReceipt,
    ): ReceiptSaveResult {
        val products = productDao.listActiveOnce()

        val receiptEntity = ReceiptEntity(
            accessKey = accessKey,
            sourceUrl = receipt.sourceUrl,
            merchantName = receipt.merchantName,
            merchantCnpj = receipt.merchantCnpj,
            merchantAddress = receipt.merchantAddress,
            number = receipt.number,
            series = receipt.series,
            issuedAt = receipt.issuedAt,
            totalAmount = receipt.totalAmount?.toPlainString(),
        )

        val items = receipt.items.mapIndexed { index, item ->
            ReceiptItemEntity(
                receiptId = 0,
                lineNumber = index + 1,
                fiscalDescription = item.description,
                itemCode = item.code,
                quantity = item.quantity?.toPlainString(),
                unit = item.unit,
                unitPrice = item.unitPrice?.toPlainString(),
                totalAmount = item.total?.toPlainString(),
                productId = findExactProductMatch(
                    fiscalDescription = item.description,
                    products = products,
                )?.id,
            )
        }

        val insertResult = receiptDao.insertReceiptWithItems(receiptEntity, items)

        return ReceiptSaveResult(
            receiptId = insertResult.receiptId,
            inserted = insertResult.inserted,
            matchedItems = items.count { it.productId != null },
            totalItems = items.size,
        )
    }

    private fun findExactProductMatch(
        fiscalDescription: String,
        products: List<ProductEntity>,
    ): ProductEntity? {
        val fiscalKey = ProductNormalizer.searchKey(fiscalDescription)

        return products.firstOrNull { product ->
            val registeredFiscal = product.fiscalDescription
                ?.let(ProductNormalizer::searchKey)

            registeredFiscal == fiscalKey ||
                ProductNormalizer.searchKey(product.normalizedName) == fiscalKey
        }
    }
}


data class ReceiptSaveResult(
    val receiptId: Long,
    val inserted: Boolean,
    val matchedItems: Int,
    val totalItems: Int,
)
