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
            firstImportedAt = insertResult.firstImportedAt,
            matchedItems = items.count { it.productId != null },
            totalItems = items.size,
        )
    }

    suspend fun findImportedReceipt(accessKey: String): ReceiptEntity? =
        receiptDao.findReceiptByAccessKey(accessKey)

    fun observeHistory(
        startDate: String,
        endDate: String,
    ) = receiptDao.observeHistory(startDate, endDate)

    suspend fun saveItemCorrection(
        item: HistoryItemRow,
        description: String,
        quantity: String,
        unit: String,
        unitPrice: String,
        totalAmount: String,
    ): ItemCorrectionResult {
        val normalizedDescription = description.trim()
        if (normalizedDescription.isBlank()) {
            return ItemCorrectionResult.Error("A descrição do item não pode ficar vazia.")
        }

        val normalizedQuantity = normalizeOptionalDecimal(quantity)
            ?: return ItemCorrectionResult.Error("Quantidade inválida.")
        val normalizedUnitPrice = normalizeOptionalDecimal(unitPrice)
            ?: return ItemCorrectionResult.Error("Valor unitário inválido.")
        val normalizedTotal = normalizeOptionalDecimal(totalAmount)
            ?: return ItemCorrectionResult.Error("Valor total inválido.")

        val normalizedUnit = unit.trim().ifBlank { item.unit.orEmpty() }

        val correctedDescription = normalizedDescription
            .takeUnless { it == item.fiscalDescription }
        val correctedQuantity = normalizedQuantity
            .takeUnless { it == item.quantity.orEmpty() }
        val correctedUnit = normalizedUnit
            .takeUnless { it == item.unit.orEmpty() }
        val correctedUnitPrice = normalizedUnitPrice
            .takeUnless { it == item.unitPrice.orEmpty() }
        val correctedTotalAmount = normalizedTotal
            .takeUnless { it == item.totalAmount.orEmpty() }

        val hasCorrection = listOf(
            correctedDescription,
            correctedQuantity,
            correctedUnit,
            correctedUnitPrice,
            correctedTotalAmount,
        ).any { it != null }

        val updated = receiptDao.updateItemCorrection(
            itemId = item.itemId,
            correctedDescription = correctedDescription,
            correctedQuantity = correctedQuantity,
            correctedUnit = correctedUnit,
            correctedUnitPrice = correctedUnitPrice,
            correctedTotalAmount = correctedTotalAmount,
            correctedAt = if (hasCorrection) System.currentTimeMillis() else null,
        )

        return if (updated > 0) {
            ItemCorrectionResult.Success(hasCorrection)
        } else {
            ItemCorrectionResult.Error("O item não foi encontrado no histórico.")
        }
    }

    suspend fun clearItemCorrection(itemId: Long): ItemCorrectionResult {
        val updated = receiptDao.clearItemCorrection(itemId)
        return if (updated > 0) {
            ItemCorrectionResult.Success(hasCorrection = false)
        } else {
            ItemCorrectionResult.Error("O item não foi encontrado no histórico.")
        }
    }

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

        val updatedItems = if (itemCode != null) {
            receiptDao.updateEquivalentItemsByCode(
                merchantCnpjDigits = merchantCnpj,
                itemCode = itemCode,
                productId = productId,
            )
        } else {
            receiptDao.updateEquivalentItemsByDescription(
                merchantCnpjDigits = merchantCnpj,
                fiscalDescription = item.fiscalDescription,
                productId = productId,
            )
        }

        if (updatedItems == 0) {
            receiptDao.updateItemProduct(
                itemId = item.itemId,
                productId = productId,
            )
        }

        return ProductLinkResult.Success(
            linkId = savedLink.id,
            updatedItems = if (updatedItems > 0) updatedItems else 1,
        )
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

    private fun normalizeOptionalDecimal(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return ""

        val normalized = trimmed
            .replace(".", "")
            .replace(",", ".")
            .replace(Regex("""[^0-9.-]"""), "")

        return normalized.toBigDecimalOrNull()?.stripTrailingZeros()?.toPlainString()
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
    val firstImportedAt: Long,
    val matchedItems: Int,
    val totalItems: Int,
)

sealed interface ProductLinkResult {
    data class Success(
        val linkId: Long,
        val updatedItems: Int,
    ) : ProductLinkResult

    data class Error(val message: String) : ProductLinkResult
}


sealed interface ItemCorrectionResult {
    data class Success(val hasCorrection: Boolean) : ItemCorrectionResult
    data class Error(val message: String) : ItemCorrectionResult
}
