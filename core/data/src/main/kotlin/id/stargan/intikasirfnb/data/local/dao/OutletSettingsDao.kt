package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.OutletSettingsEntity

@Dao
interface OutletSettingsDao {
    @Query("SELECT * FROM outlet_settings WHERE outletId = :outletId")
    suspend fun getByOutletId(outletId: String): OutletSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OutletSettingsEntity)
}
