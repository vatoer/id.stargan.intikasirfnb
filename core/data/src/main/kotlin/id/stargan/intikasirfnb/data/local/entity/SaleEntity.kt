package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sales",
    foreignKeys = [ForeignKey(entity = OutletEntity::class, parentColumns = ["id"], childColumns = ["outletId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("outletId"), Index("status"), Index("tableId"), Index("channelId")]
)
data class SaleEntity(
    @PrimaryKey val id: String,
    val outletId: String,
    val channelId: String,
    val receiptNumber: String? = null,
    val tableId: String? = null,
    val externalOrderId: String? = null,
    val cashierId: String? = null,
    val customerId: String? = null,
    val status: String,
    val notes: String? = null,
    // Tax / SC / Tip snapshots
    val taxLinesJson: String? = null,
    val serviceChargeRate: String? = null,
    val serviceChargeAmount: String? = null,
    val serviceChargeIsIncluded: Boolean? = null,
    val tipAmount: String? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    // Sync metadata
    val syncStatus: String = "PENDING",
    val syncVersion: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdByTerminalId: String? = null,
    val updatedByTerminalId: String? = null,
    val deletedAt: Long? = null
)
