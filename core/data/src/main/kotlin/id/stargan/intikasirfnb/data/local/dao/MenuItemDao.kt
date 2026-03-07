package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.MenuItemEntity

@Dao
interface MenuItemDao {
    @Query("SELECT * FROM menu_items WHERE id = :id")
    suspend fun getById(id: String): MenuItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MenuItemEntity)

    @Query("DELETE FROM menu_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM menu_items WHERE tenantId = :tenantId ORDER BY sortOrder, name")
    suspend fun listByTenant(tenantId: String): List<MenuItemEntity>

    @Query("SELECT * FROM menu_items WHERE categoryId = :categoryId ORDER BY sortOrder, name")
    suspend fun listByCategory(categoryId: String): List<MenuItemEntity>

    @Query("SELECT * FROM menu_items WHERE tenantId = :tenantId AND name LIKE '%' || :query || '%' ORDER BY sortOrder, name")
    suspend fun searchByName(tenantId: String, query: String): List<MenuItemEntity>
}
