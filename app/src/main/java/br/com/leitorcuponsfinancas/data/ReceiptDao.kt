package br.com.leitorcuponsfinancas.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReceipt(receipt: ReceiptEntity): Long

    @Query("SELECT id FROM receipts WHERE accessKey = :accessKey LIMIT 1")
    suspend fun findReceiptIdByAccessKey(accessKey: String): Long?

    @Query("SELECT * FROM receipts WHERE accessKey = :accessKey LIMIT 1")
    suspend fun findReceiptByAccessKey(accessKey: String): ReceiptEntity?

    @Insert
    suspend fun insertItems(items: List<ReceiptItemEntity>)

    @Query("SELECT * FROM receipt_items WHERE receiptId = :receiptId ORDER BY lineNumber")
    suspend fun listItems(receiptId: Long): List<ReceiptItemEntity>

    @Query("SELECT COUNT(*) FROM receipts")
    suspend fun countReceipts(): Int

    @Query(
        """
        SELECT
            ri.id AS itemId,
            r.id AS receiptId,
            r.issuedDate AS issuedDate,
            r.issuedAt AS issuedAt,
            r.merchantName AS merchantName,
            r.merchantCnpj AS merchantCnpj,
            r.number AS receiptNumber,
            r.series AS receiptSeries,
            ri.fiscalDescription AS fiscalDescription,
            ri.itemCode AS itemCode,
            ri.quantity AS quantity,
            ri.unit AS unit,
            ri.unitPrice AS unitPrice,
            ri.totalAmount AS totalAmount,
            ri.productId AS productId,
            ri.correctedDescription AS correctedDescription,
            ri.correctedQuantity AS correctedQuantity,
            ri.correctedUnit AS correctedUnit,
            ri.correctedUnitPrice AS correctedUnitPrice,
            ri.correctedTotalAmount AS correctedTotalAmount,
            ri.correctedAt AS correctedAt,
            p.normalizedName AS productName,
            p.sector AS sector,
            p.category AS category,
            p.subcategory AS subcategory
        FROM receipt_items ri
        INNER JOIN receipts r ON r.id = ri.receiptId
        LEFT JOIN products p ON p.id = ri.productId
        WHERE r.issuedDate IS NOT NULL
          AND r.issuedDate BETWEEN :startDate AND :endDate
        ORDER BY r.issuedDate DESC, r.id DESC, ri.lineNumber ASC
        """,
    )
    fun observeHistory(
        startDate: String,
        endDate: String,
    ): Flow<List<HistoryItemRow>>

    @Query("UPDATE receipt_items SET productId = :productId WHERE id = :itemId")
    suspend fun updateItemProduct(
        itemId: Long,
        productId: Long,
    ): Int

    @Query(
        """
        UPDATE receipt_items
        SET correctedDescription = :correctedDescription,
            correctedQuantity = :correctedQuantity,
            correctedUnit = :correctedUnit,
            correctedUnitPrice = :correctedUnitPrice,
            correctedTotalAmount = :correctedTotalAmount,
            correctedAt = :correctedAt
        WHERE id = :itemId
        """,
    )
    suspend fun updateItemCorrection(
        itemId: Long,
        correctedDescription: String?,
        correctedQuantity: String?,
        correctedUnit: String?,
        correctedUnitPrice: String?,
        correctedTotalAmount: String?,
        correctedAt: Long?,
    ): Int

    @Query(
        """
        UPDATE receipt_items
        SET correctedDescription = NULL,
            correctedQuantity = NULL,
            correctedUnit = NULL,
            correctedUnitPrice = NULL,
            correctedTotalAmount = NULL,
            correctedAt = NULL
        WHERE id = :itemId
        """,
    )
    suspend fun clearItemCorrection(itemId: Long): Int

    @Query(
        """
        UPDATE receipt_items
        SET productId = :productId
        WHERE productId IS NULL
          AND itemCode = :itemCode
          AND receiptId IN (
              SELECT id
              FROM receipts
              WHERE REPLACE(
                  REPLACE(
                      REPLACE(COALESCE(merchantCnpj, ''), '.', ''),
                      '/',
                      ''
                  ),
                  '-',
                  ''
              ) = :merchantCnpjDigits
          )
        """,
    )
    suspend fun updateEquivalentItemsByCode(
        merchantCnpjDigits: String,
        itemCode: String,
        productId: Long,
    ): Int

    @Query(
        """
        UPDATE receipt_items
        SET productId = :productId
        WHERE productId IS NULL
          AND UPPER(TRIM(fiscalDescription)) = UPPER(TRIM(:fiscalDescription))
          AND receiptId IN (
              SELECT id
              FROM receipts
              WHERE REPLACE(
                  REPLACE(
                      REPLACE(COALESCE(merchantCnpj, ''), '.', ''),
                      '/',
                      ''
                  ),
                  '-',
                  ''
              ) = :merchantCnpjDigits
          )
        """,
    )
    suspend fun updateEquivalentItemsByDescription(
        merchantCnpjDigits: String,
        fiscalDescription: String,
        productId: Long,
    ): Int

    @Transaction
    suspend fun insertReceiptWithItems(
        receipt: ReceiptEntity,
        items: List<ReceiptItemEntity>,
    ): ReceiptInsertResult {
        val insertedId = insertReceipt(receipt)

        if (insertedId == -1L) {
            val existing = findReceiptByAccessKey(receipt.accessKey)
                ?: error("NFC-e duplicada sem registro existente localizado.")

            return ReceiptInsertResult(
                receiptId = existing.id,
                inserted = false,
                firstImportedAt = existing.createdAt,
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
            firstImportedAt = receipt.createdAt,
        )
    }
}

data class ReceiptInsertResult(
    val receiptId: Long,
    val inserted: Boolean,
    val firstImportedAt: Long,
)
