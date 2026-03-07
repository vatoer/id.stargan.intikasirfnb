package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.MenuItemModifierGroupEntity

@Dao
interface MenuItemModifierGroupDao {
    @Query("SELECT * FROM menu_item_modifier_groups WHERE menuItemId = :menuItemId ORDER BY sortOrder")
    suspend fun listByMenuItem(menuItemId: String): List<MenuItemModifierGroupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MenuItemModifierGroupEntity)

    @Query("DELETE FROM menu_item_modifier_groups WHERE menuItemId = :menuItemId AND modifierGroupId = :modifierGroupId")
    suspend fun delete(menuItemId: String, modifierGroupId: String)

    @Query("DELETE FROM menu_item_modifier_groups WHERE menuItemId = :menuItemId")
    suspend fun deleteAllForItem(menuItemId: String)
}
