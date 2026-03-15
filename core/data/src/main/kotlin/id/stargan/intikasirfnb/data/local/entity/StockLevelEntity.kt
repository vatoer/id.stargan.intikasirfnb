package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_levels",
    indices = [Index("productId", "outletId", unique = true), Index("outletId")]
)
data class StockLevelEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val outletId: String,
    val productName: String = "",
    val quantity: String, // BigDecimal as String
    val unitOfMeasure: String = "PCS",
    val lowStockThreshold: String = "0",
    // Sync metadata
    val syncStatus: String = "PENDING",
    val syncVersion: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdByTerminalId: String? = null,
    val updatedByTerminalId: String? = null,
    val deletedAt: Long? = null
)
