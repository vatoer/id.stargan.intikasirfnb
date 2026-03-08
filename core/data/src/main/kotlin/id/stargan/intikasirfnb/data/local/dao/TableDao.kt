package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.TableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TableDao {
    @Query("SELECT * FROM tables WHERE id = :id")
    suspend fun getById(id: String): TableEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TableEntity)

    @Query("SELECT * FROM tables WHERE outletId = :outletId AND isActive = 1 AND deletedAt IS NULL ORDER BY section, name")
    suspend fun listByOutlet(outletId: String): List<TableEntity>

    @Query("SELECT * FROM tables WHERE outletId = :outletId AND isActive = 1 AND deletedAt IS NULL ORDER BY section, name")
    fun streamByOutlet(outletId: String): Flow<List<TableEntity>>

    @Query("SELECT * FROM tables WHERE outletId = :outletId AND isActive = 1 AND currentSaleId IS NULL AND deletedAt IS NULL ORDER BY section, name")
    suspend fun listAvailable(outletId: String): List<TableEntity>

    @Query("SELECT * FROM tables WHERE outletId = :outletId AND isActive = 1 AND currentSaleId IS NOT NULL AND deletedAt IS NULL ORDER BY section, name")
    suspend fun listOccupied(outletId: String): List<TableEntity>

    @Query("UPDATE tables SET currentSaleId = :saleId, updatedAt = :now WHERE id = :tableId")
    suspend fun updateCurrentSaleId(tableId: String, saleId: String?, now: Long = System.currentTimeMillis())

    @Query("UPDATE tables SET currentSaleId = NULL, updatedAt = :now WHERE currentSaleId = :saleId")
    suspend fun releaseBySaleId(saleId: String, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM tables WHERE currentSaleId = :saleId AND deletedAt IS NULL LIMIT 1")
    suspend fun findBySaleId(saleId: String): TableEntity?

    @Query("UPDATE tables SET deletedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM tables WHERE outletId = :outletId AND isActive = 1 AND deletedAt IS NULL")
    suspend fun countByOutlet(outletId: String): Int

    @Query("SELECT DISTINCT section FROM tables WHERE outletId = :outletId AND isActive = 1 AND section IS NOT NULL AND deletedAt IS NULL ORDER BY section")
    suspend fun listSections(outletId: String): List<String>
}
