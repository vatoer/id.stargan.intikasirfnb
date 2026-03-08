package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "price_lists",
    indices = [Index("tenantId")]
)
data class PriceListEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val name: String,
    val description: String? = null,
    val isActive: Boolean = true,
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
