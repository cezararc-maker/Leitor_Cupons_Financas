package br.com.leitorcuponsfinancas.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payment_allocations",
    indices = [
        Index(value = ["receiptId"]),
    ],
)
data class PaymentAllocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val receiptId: Long,
    val method: String,
    val amount: String,
    val installmentCount: Int? = null,
    val benefitType: String? = null,
    val instrumentId: Long? = null,
    val firstDueDate: String? = null,
    val source: String = "USER",
    val confidence: Double? = null,
    val cardBrand: String? = null,
    val cardLast4: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
