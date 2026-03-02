package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    foreignKeys = [ForeignKey(entity = TenantEntity::class, parentColumns = ["id"], childColumns = ["tenantId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("tenantId"), Index("parentId")]
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val name: String,
    val parentId: String? = null,
    val sortOrder: Int = 0,
    val isActive: Boolean = true
)
