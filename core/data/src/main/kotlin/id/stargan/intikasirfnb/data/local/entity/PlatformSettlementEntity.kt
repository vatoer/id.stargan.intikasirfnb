package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "platform_settlements",
    indices = [Index("outletId"), Index("channelId"), Index("status")]
)
data class PlatformSettlementEntity(
    @PrimaryKey val id: String,
    val outletId: String,
    val channelId: String,
    val platformName: String,
    val saleIdsJson: String, // JSON array of sale IDs
    val expectedAmountAmount: String,
    val expectedAmountCurrency: String = "IDR",
    val settledAmountAmount: String? = null,
    val settledAmountCurrency: String? = null,
    val commissionTotalAmount: String,
    val commissionTotalCurrency: String = "IDR",
    val status: String, // SettlementStatus enum
    val platformReference: String? = null,
    val settlementDate: Long? = null,
    val notes: String? = null,
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
