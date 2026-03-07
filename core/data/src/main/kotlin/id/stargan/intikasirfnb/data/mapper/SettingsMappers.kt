package id.stargan.intikasirfnb.data.mapper

import id.stargan.intikasirfnb.data.local.entity.OutletSettingsEntity
import id.stargan.intikasirfnb.data.local.entity.TaxConfigEntity
import id.stargan.intikasirfnb.data.local.entity.TenantSettingsEntity
import id.stargan.intikasirfnb.data.local.entity.TerminalSettingsEntity
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.identity.TerminalId
import id.stargan.intikasirfnb.domain.settings.LogoSize
import id.stargan.intikasirfnb.domain.settings.NumberingSequenceConfig
import id.stargan.intikasirfnb.domain.settings.OutletProfile
import id.stargan.intikasirfnb.domain.settings.OutletSettings
import id.stargan.intikasirfnb.domain.settings.PaperWidth
import id.stargan.intikasirfnb.domain.settings.PrinterConfig
import id.stargan.intikasirfnb.domain.settings.PrinterConnectionType
import id.stargan.intikasirfnb.domain.settings.ReceiptBarcodeType
import id.stargan.intikasirfnb.domain.settings.ReceiptBodyConfig
import id.stargan.intikasirfnb.domain.settings.ReceiptConfig
import id.stargan.intikasirfnb.domain.settings.ReceiptFooterConfig
import id.stargan.intikasirfnb.domain.settings.ReceiptHeaderConfig
import id.stargan.intikasirfnb.domain.settings.ServiceChargeConfig
import id.stargan.intikasirfnb.domain.settings.TaxConfig
import id.stargan.intikasirfnb.domain.settings.TaxConfigId
import id.stargan.intikasirfnb.domain.settings.TaxScope
import id.stargan.intikasirfnb.domain.settings.TenantSettings
import id.stargan.intikasirfnb.domain.settings.TerminalSettings
import id.stargan.intikasirfnb.domain.settings.TipConfig
import java.math.BigDecimal

// --- TenantSettings ---

fun TenantSettingsEntity.toDomain(): TenantSettings = TenantSettings(
    tenantId = TenantId(tenantId),
    defaultCurrencyCode = defaultCurrencyCode,
    receiptNumbering = receiptNumberingPrefix?.let {
        NumberingSequenceConfig(prefix = it, nextNumber = receiptNumberingNext)
    },
    invoiceNumbering = invoiceNumberingPrefix?.let {
        NumberingSequenceConfig(prefix = it, nextNumber = invoiceNumberingNext)
    },
    syncEnabled = syncEnabled
)

fun TenantSettings.toEntity(): TenantSettingsEntity = TenantSettingsEntity(
    tenantId = tenantId.value,
    defaultCurrencyCode = defaultCurrencyCode,
    receiptNumberingPrefix = receiptNumbering?.prefix,
    receiptNumberingNext = receiptNumbering?.nextNumber ?: 1L,
    invoiceNumberingPrefix = invoiceNumbering?.prefix,
    invoiceNumberingNext = invoiceNumbering?.nextNumber ?: 1L,
    syncEnabled = syncEnabled
)

// --- JSON helpers ---

private fun parseStringList(json: String): List<String> {
    if (json == "[]") return emptyList()
    return json.removeSurrounding("[", "]")
        .split("\",\"")
        .map { it.trim().removeSurrounding("\"") }
        .filter { it.isNotBlank() }
}

private fun toStringListJson(list: List<String>): String {
    if (list.isEmpty()) return "[]"
    return list.joinToString(",", "[", "]") { "\"$it\"" }
}

private fun parseBigDecimalList(json: String): List<BigDecimal> {
    if (json == "[]") return emptyList()
    return json.removeSurrounding("[", "]")
        .split(",")
        .filter { it.isNotBlank() }
        .map { BigDecimal(it.trim()) }
}

// --- OutletSettings ---

fun OutletSettingsEntity.toDomain(): OutletSettings = OutletSettings(
    outletId = OutletId(outletId),
    tenantId = TenantId(tenantId),
    timeZoneId = timeZoneId,
    outletProfile = OutletProfile(
        name = outletName,
        address = outletAddress,
        phone = outletPhone,
        npwp = outletNpwp,
        logoImagePath = outletLogoImagePath
    ),
    serviceCharge = ServiceChargeConfig(
        isEnabled = serviceChargeEnabled,
        rate = BigDecimal(serviceChargeRate),
        isIncludedInPrice = serviceChargeIncludedInPrice
    ),
    tip = TipConfig(
        isEnabled = tipEnabled,
        suggestedPercentages = parseBigDecimalList(tipSuggestedPercentagesJson),
        allowCustomAmount = tipAllowCustomAmount
    ),
    receipt = ReceiptConfig(
        header = ReceiptHeaderConfig(
            showLogo = receiptShowLogo,
            logoSize = LogoSize.valueOf(receiptLogoSize),
            showBusinessName = receiptShowBusinessName,
            showAddress = receiptShowAddress,
            showPhone = receiptShowPhone,
            showNpwp = receiptShowNpwp,
            customHeaderLines = parseStringList(receiptCustomHeaderLinesJson)
        ),
        body = ReceiptBodyConfig(
            showItemNotes = receiptShowItemNotes,
            showDiscountBreakdown = receiptShowDiscountBreakdown,
            showTaxDetail = receiptShowTaxDetail,
            showServiceChargeDetail = receiptShowServiceChargeDetail,
            showPaymentMethod = receiptShowPaymentMethod,
            showCashierName = receiptShowCashierName,
            showOrderNumber = receiptShowOrderNumber,
            showTableNumber = receiptShowTableNumber,
            showCustomerName = receiptShowCustomerName
        ),
        footer = ReceiptFooterConfig(
            customFooterText = receiptCustomFooterText,
            showSocialMedia = receiptShowSocialMedia,
            socialMediaText = receiptSocialMediaText,
            barcodeType = ReceiptBarcodeType.valueOf(receiptBarcodeType),
            showThankYouMessage = receiptShowThankYouMessage,
            thankYouMessage = receiptThankYouMessage
        ),
        paperWidth = PaperWidth.valueOf(receiptPaperWidth)
    )
)

fun OutletSettings.toEntity(): OutletSettingsEntity = OutletSettingsEntity(
    outletId = outletId.value,
    tenantId = tenantId.value,
    timeZoneId = timeZoneId,
    // Outlet Profile
    outletName = outletProfile.name,
    outletAddress = outletProfile.address,
    outletPhone = outletProfile.phone,
    outletNpwp = outletProfile.npwp,
    outletLogoImagePath = outletProfile.logoImagePath,
    // Service Charge
    serviceChargeEnabled = serviceCharge.isEnabled,
    serviceChargeRate = serviceCharge.rate.toPlainString(),
    serviceChargeIncludedInPrice = serviceCharge.isIncludedInPrice,
    // Tip
    tipEnabled = tip.isEnabled,
    tipSuggestedPercentagesJson = "[${tip.suggestedPercentages.joinToString(",") { it.toPlainString() }}]",
    tipAllowCustomAmount = tip.allowCustomAmount,
    // Receipt — Header
    receiptShowLogo = receipt.header.showLogo,
    receiptLogoSize = receipt.header.logoSize.name,
    receiptShowBusinessName = receipt.header.showBusinessName,
    receiptShowAddress = receipt.header.showAddress,
    receiptShowPhone = receipt.header.showPhone,
    receiptShowNpwp = receipt.header.showNpwp,
    receiptCustomHeaderLinesJson = toStringListJson(receipt.header.customHeaderLines),
    // Receipt — Body
    receiptShowItemNotes = receipt.body.showItemNotes,
    receiptShowDiscountBreakdown = receipt.body.showDiscountBreakdown,
    receiptShowTaxDetail = receipt.body.showTaxDetail,
    receiptShowServiceChargeDetail = receipt.body.showServiceChargeDetail,
    receiptShowPaymentMethod = receipt.body.showPaymentMethod,
    receiptShowCashierName = receipt.body.showCashierName,
    receiptShowOrderNumber = receipt.body.showOrderNumber,
    receiptShowTableNumber = receipt.body.showTableNumber,
    receiptShowCustomerName = receipt.body.showCustomerName,
    // Receipt — Footer
    receiptCustomFooterText = receipt.footer.customFooterText,
    receiptShowSocialMedia = receipt.footer.showSocialMedia,
    receiptSocialMediaText = receipt.footer.socialMediaText,
    receiptBarcodeType = receipt.footer.barcodeType.name,
    receiptShowThankYouMessage = receipt.footer.showThankYouMessage,
    receiptThankYouMessage = receipt.footer.thankYouMessage,
    // Receipt — Paper
    receiptPaperWidth = receipt.paperWidth.name
)

// --- TaxConfig ---

fun TaxConfigEntity.toDomain(): TaxConfig = TaxConfig(
    id = TaxConfigId(id),
    tenantId = TenantId(tenantId),
    name = name,
    rate = BigDecimal(rate),
    scope = TaxScope.valueOf(scope),
    isIncludedInPrice = isIncludedInPrice,
    isActive = isActive,
    sortOrder = sortOrder
)

fun TaxConfig.toEntity(): TaxConfigEntity = TaxConfigEntity(
    id = id.value,
    tenantId = tenantId.value,
    name = name,
    rate = rate.toPlainString(),
    scope = scope.name,
    isIncludedInPrice = isIncludedInPrice,
    isActive = isActive,
    sortOrder = sortOrder
)

// --- TerminalSettings ---

fun TerminalSettingsEntity.toDomain(): TerminalSettings = TerminalSettings(
    terminalId = TerminalId(terminalId),
    outletId = OutletId(outletId),
    printer = PrinterConfig(
        connectionType = PrinterConnectionType.valueOf(printerConnectionType),
        address = printerAddress,
        name = printerName,
        autoCut = printerAutoCut,
        printDensity = printerDensity,
        autoPrintReceipt = autoPrintReceipt,
        autoPrintKitchenTicket = autoPrintKitchenTicket,
        receiptCopies = receiptCopies,
        kitchenTicketCopies = kitchenTicketCopies,
        openCashDrawer = openCashDrawer
    )
)

fun TerminalSettings.toEntity(): TerminalSettingsEntity = TerminalSettingsEntity(
    terminalId = terminalId.value,
    outletId = outletId.value,
    printerConnectionType = printer.connectionType.name,
    printerAddress = printer.address,
    printerName = printer.name,
    printerAutoCut = printer.autoCut,
    printerDensity = printer.printDensity,
    autoPrintReceipt = printer.autoPrintReceipt,
    autoPrintKitchenTicket = printer.autoPrintKitchenTicket,
    receiptCopies = printer.receiptCopies,
    kitchenTicketCopies = printer.kitchenTicketCopies,
    openCashDrawer = printer.openCashDrawer
)
