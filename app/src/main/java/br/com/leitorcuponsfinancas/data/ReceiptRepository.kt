package br.com.leitorcuponsfinancas.data

import br.com.leitorcuponsfinancas.domain.NfceReceipt
import br.com.leitorcuponsfinancas.domain.ProductNormalizer

class ReceiptRepository(
    private val receiptDao: ReceiptDao,
    private val productDao: ProductDao,
    private val linkDao: MerchantProductLinkDao,
) {

    suspend fun save(
        accessKey: String,
        receipt: NfceReceipt,
    ): ReceiptSaveResult {
        val products = productDao.listActiveOnce()
        val merchantCnpj = normalizeCnpj(receipt.merchantCnpj)

        val receiptEntity = ReceiptEntity(
            accessKey = accessKey,
            sourceUrl = receipt.sourceUrl,
            merchantName = receipt.merchantName,
            merchantCnpj = receipt.merchantCnpj,
            merchantAddress = receipt.merchantAddress,
            number = receipt.number,
            series = receipt.series,
            issuedAt = receipt.issuedAt,
            issuedDate = parseIsoDate(receipt.issuedAt),
            totalAmount = receipt.totalAmount?.toPlainString(),
        )

        val items = receipt.items.mapIndexed { index, item ->
            val learnedProductId = merchantCnpj?.let { merchant ->
                findLearnedProductId(
                    merchantCnpj = merchant,
                    itemCode = item.code,
                    fiscalDescription = item.description,
                )
            }

            val exactProductId = findExactProductMatch(
                fiscalDescription = item.description,
                products = products,
            )?.id

            ReceiptItemEntity(
                receiptId = 0,
                lineNumber = index + 1,
                fiscalDescription = item.description,
                itemCode = item.code,
                quantity = item.quantity?.toPlainString(),
                unit = item.unit,
                unitPrice = item.unitPrice?.toPlainString(),
                totalAmount = item.total?.toPlainString(),
                productId = learnedProductId ?: exactProductId,
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

    fun observeHistory(
        startDate: String,
        endDate: String,
    ) = receiptDao.observeHistory(startDate, endDate)

    suspend fun linkHistoryItem(
        item: HistoryItemRow,
        productId: Long,
    ): ProductLinkResult {
        val merchantCnpj = normalizeCnpj(item.merchantCnpj)
            ?: return ProductLinkResult.Error(
                "Não foi possível criar o vínculo sem o CNPJ do estabelecimento.",
            )

        val fiscalSearchKey = ProductNormalizer.searchKey(item.fiscalDescription)
        val itemCode = item.itemCode?.trim()?.ifBlank { null }
        val now = System.currentTimeMillis()

        val existing = itemCode
            ?.let { linkDao.findByCode(merchantCnpj, it) }
            ?: linkDao.findByDescription(merchantCnpj, fiscalSearchKey)

        val savedLink = if (existing != null) {
            val updated = existing.copy(
                itemCode = itemCode ?: existing.itemCode,
                fiscalDescription = item.fiscalDescription,
                fiscalSearchKey = fiscalSearchKey,
                productId = productId,
                updatedAt = now,
                lastUsedAt = now,
            )
            linkDao.update(updated)
            updated
        } else {
            val candidate = MerchantProductLinkEntity(
                merchantCnpj = merchantCnpj,
                itemCode = itemCode,
                fiscalDescription = item.fiscalDescription,
                fiscalSearchKey = fiscalSearchKey,
                productId = productId,
                createdAt = now,
                updatedAt = now,
                lastUsedAt = now,
            )

            val id = linkDao.insert(candidate)
            if (id == -1L) {
                val collided = itemCode
                    ?.let { linkDao.findByCode(merchantCnpj, it) }
                    ?: linkDao.findByDescription(merchantCnpj, fiscalSearchKey)
                    ?: return ProductLinkResult.Error(
                        "Não foi possível atualizar o vínculo existente.",
                    )

                val updated = collided.copy(
                    fiscalDescription = item.fiscalDescription,
                    fiscalSearchKey = fiscalSearchKey,
                    productId = productId,
                    updatedAt = now,
                    lastUsedAt = now,
                )
                linkDao.update(updated)
                updated
            } else {
                candidate.copy(id = id)
            }
        }

        receiptDao.updateItemProduct(
            itemId = item.itemId,
            productId = productId,
        )

        return ProductLinkResult.Success(savedLink.id)
    }

    private suspend fun findLearnedProductId(
        merchantCnpj: String,
        itemCode: String?,
        fiscalDescription: String,
    ): Long? {
        val cleanCode = itemCode?.trim()?.ifBlank { null }
        val fiscalKey = ProductNormalizer.searchKey(fiscalDescription)

        val link = cleanCode
            ?.let { linkDao.findByCode(merchantCnpj, it) }
            ?: linkDao.findByDescription(merchantCnpj, fiscalKey)

        return link?.productId
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

    private fun normalizeCnpj(value: String?): String? = value
        ?.filter(Char::isDigit)
        ?.takeIf { it.isNotBlank() }

    private fun parseIsoDate(value: String?): String? {
        val match = value
            ?.let {
                Regex("""^(\d{2})/(\d{2})/(\d{4})""").find(it.trim())
            }
            ?: return null

        val day = match.groupValues[1]
        val month = match.groupValues[2]
        val year = match.groupValues[3]
        return "$year-$month-$day"
    }
}

data class ReceiptSaveResult(
    val receiptId: Long,
    val inserted: Boolean,
    val matchedItems: Int,
    val totalItems: Int,
)

sealed interface ProductLinkResult {
    data class Success(val linkId: Long) : ProductLinkResult
    data class Error(val message: String) : ProductLinkResult
}
