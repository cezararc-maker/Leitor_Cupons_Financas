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
)
