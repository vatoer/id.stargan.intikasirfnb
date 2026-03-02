package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.TenantEntity

@Dao
interface TenantDao {
    @Query("SELECT * FROM tenants WHERE id = :id")
    suspend fun getById(id: String): TenantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TenantEntity)

    @Query("SELECT * FROM tenants ORDER BY name")
    suspend fun listAll(): List<TenantEntity>
}
