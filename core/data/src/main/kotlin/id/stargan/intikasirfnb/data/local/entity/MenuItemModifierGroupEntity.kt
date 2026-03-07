package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "menu_item_modifier_groups",
    foreignKeys = [
        ForeignKey(
            entity = MenuItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["menuItemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ModifierGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["modifierGroupId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("menuItemId"), Index("modifierGroupId")]
)
data class MenuItemModifierGroupEntity(
    @PrimaryKey val id: String,
    val menuItemId: String,
    val modifierGroupId: String,
    val sortOrder: Int = 0,
    val isRequired: Boolean = false,
    val minSelection: Int = 0,
    val maxSelection: Int = 1,
    // Sync metadata
    val syncStatus: String = "PENDING",
    val syncVersion: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdByTerminalId: String? = null,
    val updatedByTerminalId: String? = null,
    val deletedAt: Long? = null
)
