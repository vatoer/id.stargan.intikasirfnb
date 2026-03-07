package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.SalesChannelEntity

@Dao
interface SalesChannelDao {
    @Query("SELECT * FROM sales_channels WHERE id = :id")
    suspend fun getById(id: String): SalesChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SalesChannelEntity)

    @Query("SELECT * FROM sales_channels WHERE tenantId = :tenantId ORDER BY sortOrder ASC, name ASC")
    suspend fun listByTenant(tenantId: String): List<SalesChannelEntity>

    @Query("DELETE FROM sales_channels WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM sales_channels WHERE tenantId = :tenantId")
    suspend fun countByTenant(tenantId: String): Int
}
