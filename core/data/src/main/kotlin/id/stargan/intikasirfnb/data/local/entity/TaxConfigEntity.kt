package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tax_configs",
    foreignKeys = [ForeignKey(entity = TenantEntity::class, parentColumns = ["id"], childColumns = ["tenantId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("tenantId")]
)
data class TaxConfigEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val name: String,
    val rate: String,
    val scope: String = "ALL_ITEMS",
    val isIncludedInPrice: Boolean = false,
    val isActive: Boolean = true,
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
