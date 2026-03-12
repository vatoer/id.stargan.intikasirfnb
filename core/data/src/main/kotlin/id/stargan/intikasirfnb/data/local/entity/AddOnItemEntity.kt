package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "addon_items",
    foreignKeys = [
        ForeignKey(
            entity = AddOnGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("groupId")]
)
data class AddOnItemEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val name: String,
    val priceAmount: String = "0",
    val priceCurrency: String = "IDR",
    val maxQty: Int = 5,
    val inventoryItemId: String? = null,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    // Sync metadata
    val syncStatus: String = "PENDING",
    val syncVersion: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdByTerminalId: String? = null,
    val updatedByTerminalId: String? = null,
    val deletedAt: Long? = null
)
