package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "outlet_settings",
    foreignKeys = [
        ForeignKey(entity = OutletEntity::class, parentColumns = ["id"], childColumns = ["outletId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TenantEntity::class, parentColumns = ["id"], childColumns = ["tenantId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("tenantId")]
)
data class OutletSettingsEntity(
    @PrimaryKey val outletId: String,
    val tenantId: String,
    val timeZoneId: String = "Asia/Jakarta",
    // Outlet Profile
    val outletName: String = "",
    val outletAddress: String? = null,
    val outletPhone: String? = null,
    val outletNpwp: String? = null,
    val outletLogoImagePath: String? = null,
    // Service Charge
    val serviceChargeEnabled: Boolean = false,
    val serviceChargeRate: String = "0",
    val serviceChargeIncludedInPrice: Boolean = false,
    // Tip
    val tipEnabled: Boolean = false,
    val tipSuggestedPercentagesJson: String = "[5,10,15]",
    val tipAllowCustomAmount: Boolean = true,
    // Receipt — Header (toggles only, data comes from outlet profile)
    val receiptShowLogo: Boolean = false,
    val receiptLogoSize: String = "MEDIUM",
    val receiptShowBusinessName: Boolean = true,
    val receiptShowAddress: Boolean = true,
    val receiptShowPhone: Boolean = true,
    val receiptShowNpwp: Boolean = false,
    val receiptCustomHeaderLinesJson: String = "[]",
    // Receipt — Body
    val receiptShowItemNotes: Boolean = true,
    val receiptShowDiscountBreakdown: Boolean = true,
    val receiptShowTaxDetail: Boolean = true,
    val receiptShowServiceChargeDetail: Boolean = true,
    val receiptShowPaymentMethod: Boolean = true,
    val receiptShowCashierName: Boolean = true,
    val receiptShowOrderNumber: Boolean = true,
    val receiptShowTableNumber: Boolean = true,
    val receiptShowCustomerName: Boolean = false,
    // Receipt — Footer
    val receiptCustomFooterText: String? = null,
    val receiptShowSocialMedia: Boolean = false,
    val receiptSocialMediaText: String? = null,
    val receiptBarcodeType: String = "NONE",
    val receiptShowThankYouMessage: Boolean = true,
    val receiptThankYouMessage: String = "Terima kasih atas kunjungan Anda",
    // Receipt — Paper
    val receiptPaperWidth: String = "THERMAL_58MM",
    // Sync metadata
    val syncStatus: String = "PENDING",
    val syncVersion: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdByTerminalId: String? = null,
    val updatedByTerminalId: String? = null,
    val deletedAt: Long? = null
)
