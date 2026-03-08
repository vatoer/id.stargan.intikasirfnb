package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sales_channels",
    indices = [Index("tenantId"), Index("channelType")]
)
data class SalesChannelEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val channelType: String,
    val name: String,
    val code: String,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val defaultOrderFlow: String = "PAY_FIRST",
    val priceAdjustmentType: String? = null,
    val priceAdjustmentValue: String? = null,
    // Platform config (null for non-platform channels)
    val platformName: String? = null,
    val commissionPercent: String? = null,
    val requiresExternalOrderId: Boolean = false,
    val autoConfirmOrder: Boolean = false,
    // Sync metadata
    val syncStatus: String = "PENDING",
    val syncVersion: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdByTerminalId: String? = null,
    val updatedByTerminalId: String? = null,
    val deletedAt: Long? = null
)
