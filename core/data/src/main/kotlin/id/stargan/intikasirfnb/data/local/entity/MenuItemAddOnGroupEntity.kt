package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "menu_item_addon_groups",
    foreignKeys = [
        ForeignKey(
            entity = MenuItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["menuItemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AddOnGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["addOnGroupId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("menuItemId"), Index("addOnGroupId")]
)
data class MenuItemAddOnGroupEntity(
    @PrimaryKey val id: String,
    val menuItemId: String,
    val addOnGroupId: String,
    val sortOrder: Int = 0,
    // Sync metadata
    val syncStatus: String = "PENDING",
    val syncVersion: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdByTerminalId: String? = null,
    val updatedByTerminalId: String? = null,
    val deletedAt: Long? = null
)
