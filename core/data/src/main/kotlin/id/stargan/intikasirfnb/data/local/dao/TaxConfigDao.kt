package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.TaxConfigEntity

@Dao
interface TaxConfigDao {
    @Query("SELECT * FROM tax_configs WHERE id = :id")
    suspend fun getById(id: String): TaxConfigEntity?

    @Query("SELECT * FROM tax_configs WHERE tenantId = :tenantId AND isActive = 1 AND deletedAt IS NULL ORDER BY sortOrder")
    suspend fun getActiveByTenant(tenantId: String): List<TaxConfigEntity>

    @Query("SELECT * FROM tax_configs WHERE tenantId = :tenantId AND deletedAt IS NULL ORDER BY sortOrder")
    suspend fun getAllByTenant(tenantId: String): List<TaxConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TaxConfigEntity)

    @Query("DELETE FROM tax_configs WHERE id = :id")
    suspend fun deleteById(id: String)
}
