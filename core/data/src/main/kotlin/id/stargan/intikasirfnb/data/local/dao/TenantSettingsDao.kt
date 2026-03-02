package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.TenantSettingsEntity

@Dao
interface TenantSettingsDao {
    @Query("SELECT * FROM tenant_settings WHERE tenantId = :tenantId")
    suspend fun getByTenantId(tenantId: String): TenantSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TenantSettingsEntity)
}
