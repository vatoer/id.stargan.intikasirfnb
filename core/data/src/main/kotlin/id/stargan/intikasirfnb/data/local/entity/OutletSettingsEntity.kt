package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "outlet_settings",
    foreignKeys = [
        ForeignKey(entity = OutletEntity::class, parentColumns = ["id"], childColumns = ["outletId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TenantEntity::class, parentColumns = ["id"], childColumns = ["tenantId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class OutletSettingsEntity(
    @PrimaryKey val outletId: String,
    val tenantId: String,
    val timeZoneId: String = "Asia/Jakarta",
    val receiptHeaderText: String? = null,
    val receiptFooterText: String? = null
)
