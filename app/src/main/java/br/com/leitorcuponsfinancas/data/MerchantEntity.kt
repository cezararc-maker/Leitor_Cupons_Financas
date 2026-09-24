package br.com.leitorcuponsfinancas.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "merchants",
    indices = [
        Index(value = ["cnpjDigits"], unique = true),
        Index(value = ["searchKey"]),
        Index(value = ["segmentNodeId"]),
    ],
)
data class MerchantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val displayName: String,
    val cnpjDigits: String? = null,
    val searchKey: String,
    val segmentNodeId: Long? = null,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
