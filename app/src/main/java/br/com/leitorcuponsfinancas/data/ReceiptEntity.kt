package br.com.leitorcuponsfinancas.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "receipts",
    indices = [
        Index(value = ["accessKey"], unique = true),
        Index(value = ["merchantId"]),
    ],
)
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accessKey: String,
    val sourceUrl: String,
    val merchantName: String? = null,
    val merchantCnpj: String? = null,
    val merchantAddress: String? = null,
    val merchantId: Long? = null,
    val number: String? = null,
    val series: String? = null,
    val issuedAt: String? = null,
    val issuedDate: String? = null,
    val totalAmount: String? = null,
    val sourceType: String = "NFCE",
    val createdById: String? = null,
    val createdByName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
