package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.AddOnItemEntity

@Dao
interface AddOnItemDao {
    @Query("SELECT * FROM addon_items WHERE groupId = :groupId ORDER BY sortOrder, name")
    suspend fun listByGroup(groupId: String): List<AddOnItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<AddOnItemEntity>)

    @Query("DELETE FROM addon_items WHERE groupId = :groupId")
    suspend fun deleteByGroup(groupId: String)
}
