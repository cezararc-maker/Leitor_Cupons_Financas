package br.com.leitorcuponsfinancas.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantDao {

    @Query("SELECT * FROM merchants WHERE active = 1 ORDER BY displayName COLLATE NOCASE")
    fun observeActive(): Flow<List<MerchantEntity>>

    @Query("SELECT * FROM merchants WHERE active = 1 ORDER BY displayName COLLATE NOCASE")
    suspend fun listActiveOnce(): List<MerchantEntity>

    @Query("SELECT * FROM merchants WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): MerchantEntity?

    @Query("SELECT * FROM merchants WHERE cnpjDigits = :cnpjDigits LIMIT 1")
    suspend fun findByCnpj(cnpjDigits: String): MerchantEntity?

    @Query(
        """
        SELECT * FROM merchants
        WHERE searchKey = :searchKey
        ORDER BY CASE WHEN cnpjDigits IS NULL THEN 0 ELSE 1 END, id
        LIMIT 1
        """,
    )
    suspend fun findBySearchKey(searchKey: String): MerchantEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(merchant: MerchantEntity): Long

    @Update
    suspend fun update(merchant: MerchantEntity)

    @Query("UPDATE merchants SET active = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun deactivate(id: Long, updatedAt: Long)
}
