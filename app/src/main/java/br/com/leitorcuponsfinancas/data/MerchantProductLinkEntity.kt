package br.com.leitorcuponsfinancas.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "merchant_product_links",
    indices = [
        Index(value = ["merchantCnpj", "itemCode"], unique = true),
        Index(value = ["merchantCnpj", "fiscalSearchKey"], unique = true),
        Index(value = ["productId"]),
    ],
)
data class MerchantProductLinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val merchantCnpj: String,
    val itemCode: String? = null,
    val fiscalDescription: String,
    val fiscalSearchKey: String,
    val productId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis(),
)
