package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.MenuItemAddOnGroupEntity

@Dao
interface MenuItemAddOnGroupDao {
    @Query("SELECT * FROM menu_item_addon_groups WHERE menuItemId = :menuItemId ORDER BY sortOrder")
    suspend fun listByMenuItem(menuItemId: String): List<MenuItemAddOnGroupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MenuItemAddOnGroupEntity)

    @Query("DELETE FROM menu_item_addon_groups WHERE menuItemId = :menuItemId AND addOnGroupId = :addOnGroupId")
    suspend fun delete(menuItemId: String, addOnGroupId: String)

    @Query("DELETE FROM menu_item_addon_groups WHERE menuItemId = :menuItemId")
    suspend fun deleteAllForItem(menuItemId: String)
}
