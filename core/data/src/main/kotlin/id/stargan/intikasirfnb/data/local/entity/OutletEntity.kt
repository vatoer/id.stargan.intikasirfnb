package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "outlets",
    foreignKeys = [ForeignKey(entity = TenantEntity::class, parentColumns = ["id"], childColumns = ["tenantId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("tenantId")]
)
data class OutletEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val name: String,
    val address: String? = null,
    val isActive: Boolean = true
)
