package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.PaymentEntity

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE saleId = :saleId")
    suspend fun getBySaleId(saleId: String): List<PaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PaymentEntity>)

    @Query("DELETE FROM payments WHERE saleId = :saleId")
    suspend fun deleteBySaleId(saleId: String)
}
