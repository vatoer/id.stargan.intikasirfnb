package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.SaleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getById(id: String): SaleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SaleEntity)

    @Query("SELECT * FROM sales WHERE outletId = :outletId ORDER BY createdAtMillis DESC")
    fun streamByOutlet(outletId: String): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE outletId = :outletId ORDER BY createdAtMillis DESC LIMIT :limit")
    suspend fun listByOutlet(outletId: String, limit: Int): List<SaleEntity>

    @Query("SELECT * FROM sales WHERE outletId = :outletId AND status IN ('DRAFT', 'OPEN') ORDER BY updatedAtMillis DESC")
    suspend fun listOpenByOutlet(outletId: String): List<SaleEntity>

    @Query("SELECT * FROM sales WHERE outletId = :outletId AND status IN ('DRAFT', 'OPEN') ORDER BY updatedAtMillis DESC")
    fun streamOpenByOutlet(outletId: String): Flow<List<SaleEntity>>
}
