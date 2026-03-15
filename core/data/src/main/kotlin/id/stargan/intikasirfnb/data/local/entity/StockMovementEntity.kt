package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_movements",
    indices = [Index("productId", "outletId"), Index("outletId"), Index("createdAtMillis")]
)
data class StockMovementEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val outletId: String,
    val type: String, // StockMovementType enum name
    val quantity: String, // BigDecimal as String (positive for IN, negative for OUT)
    val notes: String? = null,
    val referenceType: String? = null,
    val referenceId: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    // Sync metadata
    val syncStatus: String = "PENDING",
    val syncVersion: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdByTerminalId: String? = null,
    val updatedByTerminalId: String? = null,
    val deletedAt: Long? = null
)
