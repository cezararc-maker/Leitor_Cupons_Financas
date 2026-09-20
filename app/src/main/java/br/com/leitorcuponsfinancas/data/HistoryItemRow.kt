package br.com.leitorcuponsfinancas.data

data class HistoryItemRow(
    val itemId: Long,
    val receiptId: Long,
    val issuedDate: String?,
    val issuedAt: String?,
    val merchantName: String?,
    val merchantCnpj: String?,
    val receiptNumber: String?,
    val receiptSeries: String?,
    val sourceType: String,
    val createdByName: String?,
    val fiscalDescription: String,
    val itemCode: String?,
    val quantity: String?,
    val unit: String?,
    val unitPrice: String?,
    val totalAmount: String?,
    val productId: Long?,
    val productName: String?,
    val sector: String?,
    val category: String?,
    val subcategory: String?,
    val correctedDescription: String?,
    val correctedQuantity: String?,
    val correctedUnit: String?,
    val correctedUnitPrice: String?,
    val correctedTotalAmount: String?,
    val correctedByName: String?,
    val correctedAt: Long?,
) {
    val displayDescription: String
        get() = correctedDescription ?: fiscalDescription

    val displayQuantity: String?
        get() = correctedQuantity ?: quantity

    val displayUnit: String?
        get() = correctedUnit ?: unit

    val displayUnitPrice: String?
        get() = correctedUnitPrice ?: unitPrice

    val displayTotalAmount: String?
        get() = correctedTotalAmount ?: totalAmount

    val manuallyEdited: Boolean
        get() = correctedAt != null
}
