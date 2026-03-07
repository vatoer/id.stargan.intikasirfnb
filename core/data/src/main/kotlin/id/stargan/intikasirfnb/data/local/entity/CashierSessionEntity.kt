package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cashier_sessions",
    foreignKeys = [ForeignKey(entity = OutletEntity::class, parentColumns = ["id"], childColumns = ["outletId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("outletId"), Index("terminalId"), Index("status")]
)
data class CashierSessionEntity(
    @PrimaryKey val id: String,
    val terminalId: String,
    val outletId: String,
    val userId: String,
    val openAtMillis: Long,
    val closeAtMillis: Long? = null,
    val openingFloatAmount: String,
    val openingFloatCurrency: String = "IDR",
    val closingCashAmount: String? = null,
    val closingCashCurrency: String? = null,
    val expectedCashAmount: String? = null,
    val expectedCashCurrency: String? = null,
    val notes: String? = null,
    val status: String,
    // Sync metadata
    val syncStatus: String = "PENDING",
    val syncVersion: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdByTerminalId: String? = null,
    val updatedByTerminalId: String? = null,
    val deletedAt: Long? = null
)
