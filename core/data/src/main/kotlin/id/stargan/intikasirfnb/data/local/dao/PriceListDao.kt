package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import id.stargan.intikasirfnb.data.local.entity.PriceListEntity
import id.stargan.intikasirfnb.data.local.entity.PriceListEntryEntity

@Dao
interface PriceListDao {

    @Query("SELECT * FROM price_lists WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): PriceListEntity?

    @Query("SELECT * FROM price_list_entries WHERE priceListId = :priceListId")
    suspend fun getEntries(priceListId: String): List<PriceListEntryEntity>

    @Query("SELECT * FROM price_list_entries WHERE priceListId = :priceListId AND productId = :productId")
    suspend fun getEntry(priceListId: String, productId: String): PriceListEntryEntity?

    @Query("SELECT * FROM price_lists WHERE tenantId = :tenantId AND deletedAt IS NULL ORDER BY name")
    suspend fun listByTenant(tenantId: String): List<PriceListEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceList(entity: PriceListEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<PriceListEntryEntity>)

    @Query("DELETE FROM price_list_entries WHERE priceListId = :priceListId")
    suspend fun deleteEntries(priceListId: String)

    @Query("DELETE FROM price_lists WHERE id = :id")
    suspend fun delete(id: String)

    @Transaction
    suspend fun savePriceListWithEntries(entity: PriceListEntity, entries: List<PriceListEntryEntity>) {
        insertPriceList(entity)
        deleteEntries(entity.id)
        if (entries.isNotEmpty()) {
            insertEntries(entries)
        }
    }
}
