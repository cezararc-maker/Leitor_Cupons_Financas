package br.com.leitorcuponsfinancas.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "taxonomy_nodes",
    indices = [
        Index(value = ["stableKey"], unique = true),
        Index(value = ["parentId"]),
        Index(value = ["level"]),
    ],
)
data class TaxonomyNodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stableKey: String,
    val parentId: Long? = null,
    val level: String,
    val name: String,
    val searchKey: String,
    val builtIn: Boolean = false,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
