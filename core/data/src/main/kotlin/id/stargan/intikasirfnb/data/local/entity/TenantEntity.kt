package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tenants")
data class TenantEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isActive: Boolean = true
)
