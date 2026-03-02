package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.OutletEntity

@Dao
interface OutletDao {
    @Query("SELECT * FROM outlets WHERE id = :id")
    suspend fun getById(id: String): OutletEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OutletEntity)

    @Query("SELECT * FROM outlets WHERE tenantId = :tenantId ORDER BY name")
    suspend fun listByTenant(tenantId: String): List<OutletEntity>
}
