package id.stargan.intikasirfnb.domain.settings

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.identity.TerminalId
import id.stargan.intikasirfnb.domain.shared.UlidGenerator
import java.math.BigDecimal

// --- IDs ---

@JvmInline
value class TaxConfigId(val value: String) {
    companion object {
        fun generate() = TaxConfigId(UlidGenerator.generate())
    }
}

// --- Tax Config (separate entity, supports multiple active taxes) ---

enum class TaxScope {
    ALL_ITEMS,
    SPECIFIC_CATEGORIES,
    SPECIFIC_ITEMS
}

data class TaxConfig(
    val id: TaxConfigId,
    val tenantId: TenantId,
    val name: String,
    val rate: BigDecimal,
    val scope: TaxScope = TaxScope.ALL_ITEMS,
    val isIncludedInPrice: Boolean = false,
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)

// --- Service Charge Config (per outlet) ---

data class ServiceChargeConfig(
    val isEnabled: Boolean = false,
    val rate: BigDecimal = BigDecimal.ZERO,
    val isIncludedInPrice: Boolean = false
)

// --- Tip Config (per outlet) ---

data class TipConfig(
    val isEnabled: Boolean = false,
    val suggestedPercentages: List<BigDecimal> = listOf(
        BigDecimal("5"), BigDecimal("10"), BigDecimal("15")
    ),
    val allowCustomAmount: Boolean = true
)

// --- Numbering ---

data class NumberingSequenceConfig(
    val prefix: String,
    val paddingLength: Int = 6,
    val nextNumber: Long = 1L
)

// --- Outlet Profile (store identity — name, contact, logo) ---

data class OutletProfile(
    val name: String = "",
    val address: String? = null,
    val phone: String? = null,
    val npwp: String? = null,
    val logoImagePath: String? = null
)

// --- Receipt Config (per outlet — same layout for all terminals in one outlet) ---

enum class PaperWidth(val mm: Int, val defaultCharsPerLine: Int) {
    THERMAL_58MM(58, 32),
    THERMAL_80MM(80, 48)
}

enum class ReceiptBarcodeType {
    NONE,
    BARCODE_CODE128,
    QR_CODE
}

enum class LogoSize {
    SMALL,
    MEDIUM,
    LARGE
}

data class ReceiptHeaderConfig(
    val showLogo: Boolean = false,
    val logoSize: LogoSize = LogoSize.MEDIUM,
    val showBusinessName: Boolean = true,
    val showAddress: Boolean = true,
    val showPhone: Boolean = true,
    val showNpwp: Boolean = false,
    val customHeaderLines: List<String> = emptyList()
)

data class ReceiptBodyConfig(
    val showItemNotes: Boolean = true,
    val showDiscountBreakdown: Boolean = true,
    val showTaxDetail: Boolean = true,
    val showServiceChargeDetail: Boolean = true,
    val showPaymentMethod: Boolean = true,
    val showCashierName: Boolean = true,
    val showOrderNumber: Boolean = true,
    val showTableNumber: Boolean = true,
    val showCustomerName: Boolean = false
)

data class ReceiptFooterConfig(
    val customFooterText: String? = null,
    val showSocialMedia: Boolean = false,
    val socialMediaText: String? = null,
    val barcodeType: ReceiptBarcodeType = ReceiptBarcodeType.NONE,
    val showThankYouMessage: Boolean = true,
    val thankYouMessage: String = "Terima kasih atas kunjungan Anda"
)

data class ReceiptConfig(
    val header: ReceiptHeaderConfig = ReceiptHeaderConfig(),
    val body: ReceiptBodyConfig = ReceiptBodyConfig(),
    val footer: ReceiptFooterConfig = ReceiptFooterConfig(),
    val paperWidth: PaperWidth = PaperWidth.THERMAL_58MM
)

// --- Printer Config (per terminal — each device has its own printer) ---

enum class PrinterConnectionType {
    NONE,
    BLUETOOTH,
    USB,
    NETWORK
}

data class PrinterConfig(
    val connectionType: PrinterConnectionType = PrinterConnectionType.NONE,
    val address: String? = null,
    val name: String? = null,
    val autoCut: Boolean = true,
    val printDensity: Int = 5,
    val autoPrintReceipt: Boolean = true,
    val autoPrintKitchenTicket: Boolean = true,
    val receiptCopies: Int = 1,
    val kitchenTicketCopies: Int = 1,
    val openCashDrawer: Boolean = false
)

// --- Tenant-level settings aggregate ---

data class TenantSettings(
    val tenantId: TenantId,
    val defaultCurrencyCode: String = "IDR",
    val receiptNumbering: NumberingSequenceConfig? = null,
    val invoiceNumbering: NumberingSequenceConfig? = null,
    val syncEnabled: Boolean = false
)

// --- Outlet-level settings aggregate ---

data class OutletSettings(
    val outletId: OutletId,
    val tenantId: TenantId,
    val timeZoneId: String = "Asia/Jakarta",
    val outletProfile: OutletProfile = OutletProfile(),
    val serviceCharge: ServiceChargeConfig = ServiceChargeConfig(),
    val tip: TipConfig = TipConfig(),
    val receipt: ReceiptConfig = ReceiptConfig()
)

// --- Terminal-level settings aggregate ---

data class TerminalSettings(
    val terminalId: TerminalId,
    val outletId: OutletId,
    val printer: PrinterConfig = PrinterConfig()
)
