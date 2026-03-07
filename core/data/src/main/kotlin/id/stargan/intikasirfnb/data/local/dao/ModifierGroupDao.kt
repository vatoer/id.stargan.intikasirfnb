package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.ModifierGroupEntity

@Dao
interface ModifierGroupDao {
    @Query("SELECT * FROM modifier_groups WHERE id = :id")
    suspend fun getById(id: String): ModifierGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ModifierGroupEntity)

    @Query("DELETE FROM modifier_groups WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM modifier_groups WHERE tenantId = :tenantId ORDER BY sortOrder, name")
    suspend fun listByTenant(tenantId: String): List<ModifierGroupEntity>
}
