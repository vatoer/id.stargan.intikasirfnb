package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.TableEntity

@Dao
interface TableDao {
    @Query("SELECT * FROM tables WHERE id = :id")
    suspend fun getById(id: String): TableEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TableEntity)

    @Query("SELECT * FROM tables WHERE outletId = :outletId AND isActive = 1 ORDER BY name")
    suspend fun listByOutlet(outletId: String): List<TableEntity>
}
