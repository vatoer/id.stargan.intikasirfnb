package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.CashierSessionEntity

@Dao
interface CashierSessionDao {
    @Query("SELECT * FROM cashier_sessions WHERE outletId = :outletId AND terminalId = :terminalId AND status = 'OPEN' LIMIT 1")
    suspend fun getCurrentSession(outletId: String, terminalId: String): CashierSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CashierSessionEntity)
}
