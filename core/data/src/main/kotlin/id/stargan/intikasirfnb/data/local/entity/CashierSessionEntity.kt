package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cashier_sessions",
    foreignKeys = [ForeignKey(entity = OutletEntity::class, parentColumns = ["id"], childColumns = ["outletId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("outletId"), Index("terminalId")]
)
data class CashierSessionEntity(
    @PrimaryKey val terminalId: String,
    val outletId: String,
    val userId: String,
    val openAtMillis: Long,
    val closeAtMillis: Long? = null,
    val openingFloatAmount: String,
    val openingFloatCurrency: String = "IDR",
    val status: String
)
