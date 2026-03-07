package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.TerminalEntity

@Dao
interface TerminalDao {
    @Query("SELECT * FROM terminals WHERE id = :id")
    suspend fun getById(id: String): TerminalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TerminalEntity)

    @Query("SELECT * FROM terminals WHERE outletId = :outletId ORDER BY deviceName")
    suspend fun getByOutlet(outletId: String): List<TerminalEntity>

    @Query("SELECT * FROM terminals WHERE outletId = :outletId AND status = 'ACTIVE' ORDER BY deviceName")
    suspend fun getActiveByOutlet(outletId: String): List<TerminalEntity>
}
