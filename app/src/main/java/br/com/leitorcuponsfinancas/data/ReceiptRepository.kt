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
        actor: LocalUserProfile? = null,
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
            sourceType = "NFCE",
            createdById = actor?.id,
            createdByName = actor?.displayName,
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

    suspend fun saveManualPurchase(
        merchantName: String,
        issuedDate: String,
        description: String,
        quantity: String,
        unit: String,
        unitPrice: String,
        totalAmount: String,
        productId: Long?,
        actor: LocalUserProfile,
    ): ReceiptSaveResult {
        val normalizedMerchant = merchantName.trim()
        val normalizedDescription = description.trim()

        if (normalizedMerchant.isBlank()) {
            error("Informe o estabelecimento.")
        }
        if (normalizedDescription.isBlank()) {
            error("Informe a descrição do item.")
        }

        val normalizedQuantity = normalizeOptionalDecimal(quantity)
            ?: error("Quantidade inválida.")
        val normalizedUnitPrice = normalizeOptionalDecimal(unitPrice)
            ?: error("Valor unitário inválido.")
        val normalizedTotal = normalizeOptionalDecimal(totalAmount)
            ?: error("Valor total inválido.")

        val manualId = java.util.UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val receipt = ReceiptEntity(
            accessKey = "MANUAL:$manualId",
            sourceUrl = "manual://purchase/$manualId",
            merchantName = normalizedMerchant,
            issuedAt = issuedDate,
            issuedDate = parseIsoDate(issuedDate),
            totalAmount = normalizedTotal.ifBlank { null },
            sourceType = "MANUAL",
            createdById = actor.id,
            createdByName = actor.displayName,
            createdAt = now,
        )

        val item = ReceiptItemEntity(
            receiptId = 0,
            lineNumber = 1,
            fiscalDescription = normalizedDescription,
            quantity = normalizedQuantity.ifBlank { null },
            unit = unit.trim().ifBlank { "UN" },
            unitPrice = normalizedUnitPrice.ifBlank { null },
            totalAmount = normalizedTotal.ifBlank { null },
            productId = productId,
        )

        val inserted = receiptDao.insertReceiptWithItems(receipt, listOf(item))
        return ReceiptSaveResult(
            receiptId = inserted.receiptId,
            inserted = inserted.inserted,
            firstImportedAt = inserted.firstImportedAt,
            matchedItems = if (productId != null) 1 else 0,
            totalItems = 1,
        )
    }

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
        actor: LocalUserProfile,
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

        val originalQuantity = canonicalStoredDecimal(item.quantity)
        val originalUnitPrice = canonicalStoredDecimal(item.unitPrice)
        val originalTotal = canonicalStoredDecimal(item.totalAmount)

        val normalizedUnit = unit.trim().ifBlank { item.unit.orEmpty() }

        val correctedDescription = normalizedDescription
            .takeUnless { it == item.fiscalDescription }
        val correctedQuantity = normalizedQuantity
            .takeUnless { it == originalQuantity }
        val correctedUnit = normalizedUnit
            .takeUnless { it == item.unit.orEmpty() }
        val correctedUnitPrice = normalizedUnitPrice
            .takeUnless { it == originalUnitPrice }
        val correctedTotalAmount = normalizedTotal
            .takeUnless { it == originalTotal }

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
            correctedById = if (hasCorrection) actor.id else null,
            correctedByName = if (hasCorrection) actor.displayName else null,
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

        val cleaned = trimmed.replace(Regex("""[^0-9,.-]"""), "")
        val normalized = if (cleaned.contains(",")) {
            cleaned.replace(".", "").replace(",", ".")
        } else {
            cleaned
        }

        return normalized.toBigDecimalOrNull()?.stripTrailingZeros()?.toPlainString()
    }

    private fun canonicalStoredDecimal(value: String?): String {
        val stored = value?.trim().orEmpty()
        if (stored.isBlank()) return ""
        return stored.toBigDecimalOrNull()?.stripTrailingZeros()?.toPlainString() ?: stored
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
