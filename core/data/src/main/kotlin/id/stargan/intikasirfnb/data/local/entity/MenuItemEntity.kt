package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "menu_items",
    foreignKeys = [
        ForeignKey(entity = TenantEntity::class, parentColumns = ["id"], childColumns = ["tenantId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("tenantId"), Index("categoryId")]
)
data class MenuItemEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val categoryId: String,
    val name: String,
    val description: String? = null,
    val imageUri: String? = null,
    val basePriceAmount: String,
    val basePriceCurrency: String = "IDR",
    val taxCode: String? = null,
    val recipeJson: String? = null,
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
