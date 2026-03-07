package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.ModifierOptionEntity

@Dao
interface ModifierOptionDao {
    @Query("SELECT * FROM modifier_options WHERE groupId = :groupId ORDER BY sortOrder, name")
    suspend fun listByGroup(groupId: String): List<ModifierOptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ModifierOptionEntity>)

    @Query("DELETE FROM modifier_options WHERE groupId = :groupId")
    suspend fun deleteByGroup(groupId: String)
}
