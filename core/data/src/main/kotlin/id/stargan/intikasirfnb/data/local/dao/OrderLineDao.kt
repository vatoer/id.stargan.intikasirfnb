package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.OrderLineEntity

@Dao
interface OrderLineDao {
    @Query("SELECT * FROM order_lines WHERE saleId = :saleId")
    suspend fun getBySaleId(saleId: String): List<OrderLineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<OrderLineEntity>)

    @Query("DELETE FROM order_lines WHERE saleId = :saleId")
    suspend fun deleteBySaleId(saleId: String)
}
