package br.com.leitorcuponsfinancas.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaxonomyDao {

    @Query("SELECT * FROM taxonomy_nodes WHERE active = 1 ORDER BY level, name COLLATE NOCASE")
    fun observeActiveNodes(): Flow<List<TaxonomyNodeEntity>>

    @Query("SELECT * FROM taxonomy_nodes ORDER BY level, name COLLATE NOCASE")
    suspend fun listAllNodes(): List<TaxonomyNodeEntity>

    @Query("SELECT * FROM taxonomy_nodes WHERE active = 1 AND level = 'SEGMENT' ORDER BY name COLLATE NOCASE")
    fun observeSegments(): Flow<List<TaxonomyNodeEntity>>

    @Query("SELECT * FROM taxonomy_nodes WHERE stableKey = :stableKey LIMIT 1")
    suspend fun findByStableKey(stableKey: String): TaxonomyNodeEntity?

    @Query("SELECT * FROM taxonomy_nodes WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): TaxonomyNodeEntity?

    @Query(
        """
        SELECT * FROM taxonomy_nodes
        WHERE active = 1
          AND ((:parentId IS NULL AND parentId IS NULL) OR parentId = :parentId)
        ORDER BY name COLLATE NOCASE
        """,
    )
    suspend fun listChildren(parentId: Long?): List<TaxonomyNodeEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNode(node: TaxonomyNodeEntity): Long

    @Update
    suspend fun updateNode(node: TaxonomyNodeEntity)

    @Query("UPDATE taxonomy_nodes SET active = :active, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setNodeActive(id: Long, active: Boolean, updatedAt: Long)

    @Query("SELECT * FROM taxonomy_product_links")
    fun observeProductLinks(): Flow<List<TaxonomyProductLinkEntity>>

    @Query("SELECT * FROM taxonomy_product_links")
    suspend fun listProductLinks(): List<TaxonomyProductLinkEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProductLink(link: TaxonomyProductLinkEntity): Long

    @Query("DELETE FROM taxonomy_product_links WHERE taxonomyNodeId = :nodeId AND productId = :productId")
    suspend fun deleteProductLink(nodeId: Long, productId: Long)
}
