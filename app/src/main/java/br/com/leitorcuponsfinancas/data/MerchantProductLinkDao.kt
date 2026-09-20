package br.com.leitorcuponsfinancas.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantProductLinkDao {

    @Query(
        """
        SELECT * FROM merchant_product_links
        WHERE merchantCnpj = :merchantCnpj
          AND itemCode = :itemCode
        LIMIT 1
        """,
    )
    suspend fun findByCode(
        merchantCnpj: String,
        itemCode: String,
    ): MerchantProductLinkEntity?

    @Query(
        """
        SELECT * FROM merchant_product_links
        WHERE merchantCnpj = :merchantCnpj
          AND fiscalSearchKey = :fiscalSearchKey
        LIMIT 1
        """,
    )
    suspend fun findByDescription(
        merchantCnpj: String,
        fiscalSearchKey: String,
    ): MerchantProductLinkEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(link: MerchantProductLinkEntity): Long

    @Update
    suspend fun update(link: MerchantProductLinkEntity)

    @Query(
        """
        SELECT * FROM merchant_product_links
        ORDER BY productId, lastUsedAt DESC, fiscalDescription COLLATE NOCASE
        """,
    )
    fun observeAll(): Flow<List<MerchantProductLinkEntity>>

    @Query("SELECT COUNT(*) FROM merchant_product_links")
    suspend fun count(): Int
}
