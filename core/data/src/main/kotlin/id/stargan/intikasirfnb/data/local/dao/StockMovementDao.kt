package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.StockMovementEntity

@Dao
interface StockMovementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: StockMovementEntity)

    @Query("SELECT * FROM stock_movements WHERE productId = :productId AND outletId = :outletId ORDER BY createdAtMillis DESC LIMIT :limit")
    suspend fun listByProduct(productId: String, outletId: String, limit: Int): List<StockMovementEntity>

    @Query("SELECT * FROM stock_movements WHERE outletId = :outletId ORDER BY createdAtMillis DESC LIMIT :limit")
    suspend fun listByOutlet(outletId: String, limit: Int): List<StockMovementEntity>
}
