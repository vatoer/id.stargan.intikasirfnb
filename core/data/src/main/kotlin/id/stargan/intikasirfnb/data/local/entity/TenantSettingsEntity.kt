package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "tenant_settings",
    foreignKeys = [ForeignKey(entity = TenantEntity::class, parentColumns = ["id"], childColumns = ["tenantId"], onDelete = ForeignKey.CASCADE)]
)
data class TenantSettingsEntity(
    @PrimaryKey val tenantId: String,
    val defaultCurrencyCode: String = "IDR",
    val taxRatesJson: String = "[]",
    val receiptNumberingPrefix: String? = null,
    val receiptNumberingNext: Long = 1L,
    val invoiceNumberingPrefix: String? = null,
    val invoiceNumberingNext: Long = 1L
)
