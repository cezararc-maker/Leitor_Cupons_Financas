package br.com.leitorcuponsfinancas.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "receipt_items",
    indices = [
        Index(value = ["receiptId"]),
        Index(value = ["productId"]),
    ],
)
data class ReceiptItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val receiptId: Long,
    val lineNumber: Int,
    val fiscalDescription: String,
    val itemCode: String? = null,
    val quantity: String? = null,
    val unit: String? = null,
    val unitPrice: String? = null,
    val totalAmount: String? = null,
    val productId: Long? = null,
)
