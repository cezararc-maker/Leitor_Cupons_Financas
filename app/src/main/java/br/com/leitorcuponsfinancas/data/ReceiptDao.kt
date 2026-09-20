package br.com.leitorcuponsfinancas.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface ReceiptDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReceipt(receipt: ReceiptEntity): Long

    @Query("SELECT id FROM receipts WHERE accessKey = :accessKey LIMIT 1")
    suspend fun findReceiptIdByAccessKey(accessKey: String): Long?

    @Insert
    suspend fun insertItems(items: List<ReceiptItemEntity>)

    @Query("SELECT * FROM receipt_items WHERE receiptId = :receiptId ORDER BY lineNumber")
    suspend fun listItems(receiptId: Long): List<ReceiptItemEntity>

    @Query("SELECT COUNT(*) FROM receipts")
    suspend fun countReceipts(): Int

    @Transaction
    suspend fun insertReceiptWithItems(
        receipt: ReceiptEntity,
        items: List<ReceiptItemEntity>,
    ): ReceiptInsertResult {
        val insertedId = insertReceipt(receipt)

        if (insertedId == -1L) {
            val existingId = findReceiptIdByAccessKey(receipt.accessKey)
                ?: error("NFC-e duplicada sem registro existente localizado.")

            return ReceiptInsertResult(
                receiptId = existingId,
                inserted = false,
            )
        }

        if (items.isNotEmpty()) {
            insertItems(
                items.map { item ->
                    item.copy(receiptId = insertedId)
                },
            )
        }

        return ReceiptInsertResult(
            receiptId = insertedId,
            inserted = true,
        )
    }
}

data class ReceiptInsertResult(
    val receiptId: Long,
    val inserted: Boolean,
)
