package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.StockLevelEntity

@Dao
interface StockLevelDao {
    @Query("SELECT * FROM stock_levels WHERE id = :id")
    suspend fun getById(id: String): StockLevelEntity?

    @Query("SELECT * FROM stock_levels WHERE productId = :productId AND outletId = :outletId")
    suspend fun getByProductAndOutlet(productId: String, outletId: String): StockLevelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: StockLevelEntity)

    @Query("SELECT * FROM stock_levels WHERE outletId = :outletId ORDER BY productName")
    suspend fun listByOutlet(outletId: String): List<StockLevelEntity>
}
