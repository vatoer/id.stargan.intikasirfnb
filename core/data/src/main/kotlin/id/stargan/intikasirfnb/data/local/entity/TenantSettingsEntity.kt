package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "tenant_settings",
    foreignKeys = [ForeignKey(entity = TenantEntity::class, parentColumns = ["id"], childColumns = ["tenantId"], onDelete = ForeignKey.CASCADE)]
)
data class TenantSettingsEntity(
    @PrimaryKey val tenantId: String,
    val defaultCurrencyCode: String = "IDR",
    val receiptNumberingPrefix: String? = null,
    val receiptNumberingNext: Long = 1L,
    val invoiceNumberingPrefix: String? = null,
    val invoiceNumberingNext: Long = 1L,
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
