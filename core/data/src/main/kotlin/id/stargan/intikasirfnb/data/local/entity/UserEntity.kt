package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    foreignKeys = [ForeignKey(entity = TenantEntity::class, parentColumns = ["id"], childColumns = ["tenantId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("tenantId"), Index("email")]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val email: String,
    val displayName: String,
    val pinHash: String = "",
    val outletIdsCsv: String = "",
    val rolesCsv: String = "",
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
