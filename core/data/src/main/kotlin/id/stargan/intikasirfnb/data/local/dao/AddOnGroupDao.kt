package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.AddOnGroupEntity

@Dao
interface AddOnGroupDao {
    @Query("SELECT * FROM addon_groups WHERE id = :id")
    suspend fun getById(id: String): AddOnGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AddOnGroupEntity)

    @Query("DELETE FROM addon_groups WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM addon_groups WHERE tenantId = :tenantId ORDER BY sortOrder, name")
    suspend fun listByTenant(tenantId: String): List<AddOnGroupEntity>
}
