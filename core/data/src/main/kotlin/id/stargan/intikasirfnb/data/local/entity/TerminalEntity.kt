package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "terminals",
    foreignKeys = [
        ForeignKey(entity = TenantEntity::class, parentColumns = ["id"], childColumns = ["tenantId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = OutletEntity::class, parentColumns = ["id"], childColumns = ["outletId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("tenantId"), Index("outletId")]
)
data class TerminalEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val outletId: String,
    val deviceName: String,
    val terminalType: String = "CASHIER",
    val status: String = "ACTIVE",
    val lastSyncAtMillis: Long? = null,
    val registeredAtMillis: Long = System.currentTimeMillis(),
    val syncEnabled: Boolean = false,
    // Sync metadata
    val syncStatus: String = "PENDING",
    val syncVersion: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdByTerminalId: String? = null,
    val updatedByTerminalId: String? = null,
    val deletedAt: Long? = null
)
