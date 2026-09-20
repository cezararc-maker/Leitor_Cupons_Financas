package br.com.leitorcuponsfinancas.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fiscalDescription: String? = null,
    val normalizedName: String,
    val sector: String,
    val category: String,
    val subcategory: String? = null,
    val unit: String = "UN",
    val notes: String? = null,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
