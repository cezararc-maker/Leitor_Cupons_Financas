package br.com.leitorcuponsfinancas.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "taxonomy_product_links",
    indices = [
        Index(value = ["taxonomyNodeId", "productId"], unique = true),
        Index(value = ["productId"]),
    ],
)
data class TaxonomyProductLinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taxonomyNodeId: Long,
    val productId: Long,
    val createdAt: Long = System.currentTimeMillis(),
)
