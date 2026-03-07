package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.TerminalSettingsEntity

@Dao
interface TerminalSettingsDao {
    @Query("SELECT * FROM terminal_settings WHERE terminalId = :terminalId")
    suspend fun getByTerminalId(terminalId: String): TerminalSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TerminalSettingsEntity)
}
