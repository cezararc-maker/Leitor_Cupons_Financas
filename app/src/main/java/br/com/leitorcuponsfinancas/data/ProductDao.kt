package br.com.leitorcuponsfinancas.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products WHERE active = 1 ORDER BY normalizedName COLLATE NOCASE")
    fun observeActive(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE active = 1 ORDER BY normalizedName COLLATE NOCASE")
    suspend fun listActiveOnce(): List<ProductEntity>

    @Insert
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Query("UPDATE products SET active = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun deactivate(id: Long, updatedAt: Long)
}
