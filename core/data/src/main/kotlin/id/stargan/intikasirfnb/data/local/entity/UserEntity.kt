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
    val isActive: Boolean = true
)
