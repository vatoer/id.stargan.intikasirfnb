package id.stargan.intikasirfnb.domain.printer

import id.stargan.intikasirfnb.domain.settings.OutletProfile
import id.stargan.intikasirfnb.domain.settings.PrinterConfig
import id.stargan.intikasirfnb.domain.settings.ReceiptConfig
import id.stargan.intikasirfnb.domain.transaction.Sale
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val idrFormat = NumberFormat.getNumberInstance(Locale("id", "ID")).apply {
    maximumFractionDigits = 0
}

private fun formatMoney(amount: BigDecimal): String = idrFormat.format(amount)

fun buildSaleReceipt(
    sale: Sale,
    outletProfile: OutletProfile,
    printerConfig: PrinterConfig,
    receiptConfig: ReceiptConfig,
    cashierName: String? = null,
    channelName: String? = null,
    tableName: String? = null,
    logoRaster: RasterImage? = null
): ByteArray {
    val cpl = receiptConfig.paperWidth.defaultCharsPerLine
    val b = EscPosBuilder(cpl)
    val header = receiptConfig.header
    val body = receiptConfig.body
    val footer = receiptConfig.footer

    b.initialize()

    // --- Header ---
    b.centerAlign()

    if (header.showLogo && logoRaster != null) {
        b.rasterImage(logoRaster.data, logoRaster.widthBytes, logoRaster.heightDots)
        b.newline()
    }

    if (header.showBusinessName && outletProfile.name.isNotBlank()) {
        b.boldOn().doubleHeight()
        b.line(outletProfile.name)
        b.normalSize().boldOff()
    }

    if (header.showAddress) {
        outletProfile.address?.let { b.line(it) }
    }

    if (header.showPhone) {
        outletProfile.phone?.let { b.line("Telp: $it") }
    }

    if (header.showNpwp) {
        outletProfile.npwp?.let { b.line("NPWP: $it") }
    }

    header.customHeaderLines.forEach { line ->
        if (line.isNotBlank()) b.line(line)
    }

    b.separator('=')

    // --- Order info ---
    b.leftAlign()

    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id"))

    if (body.showOrderNumber && sale.receiptNumber != null) {
        b.twoColumnLine("No.", sale.receiptNumber)
    }

    b.twoColumnLine("Tanggal", dateFormat.format(Date(sale.createdAtMillis)))

    if (body.showCashierName && cashierName != null) {
        b.twoColumnLine("Kasir", cashierName)
    }

    if (channelName != null) {
        b.twoColumnLine("Channel", channelName)
    }

    if (body.showTableNumber && tableName != null) {
        b.twoColumnLine("Meja", tableName)
    }

    b.separator('-')

    // --- Line items ---
    for (line in sale.lines) {
        val name = line.productRef.name
        val qty = line.quantity
        val unitPrice = formatMoney(line.effectiveUnitPrice().amount)
        val lineTotal = formatMoney(line.lineTotal().amount)

        b.line(name)
        b.twoColumnLine("  ${qty} x $unitPrice", lineTotal)

        if (line.selectedModifiers.isNotEmpty()) {
            val modText = line.selectedModifiers.joinToString(", ") { it.optionName }
            b.line("  ($modText)")
        }

        if (body.showItemNotes && !line.notes.isNullOrBlank()) {
            b.line("  * ${line.notes}")
        }

        if (body.showDiscountBreakdown && line.discountAmount.isPositive()) {
            b.twoColumnLine("  Diskon", "-${formatMoney(line.discountAmount.amount)}")
        }
    }

    b.separator('-')

    // --- Totals ---
    b.twoColumnLine("Subtotal", formatMoney(sale.subtotal().amount))

    // Tax lines
    if (body.showTaxDetail) {
        sale.taxLines.forEach { tax ->
            val label = if (tax.isIncludedInPrice) {
                "${tax.taxName} (inkl.)"
            } else {
                tax.taxName
            }
            b.twoColumnLine(label, formatMoney(tax.taxAmount.amount))
        }
    }

    // Service charge
    if (body.showServiceChargeDetail) {
        sale.serviceCharge?.let { sc ->
            val label = if (sc.isIncludedInPrice) "SC (inkl.)" else "Service Charge"
            b.twoColumnLine(label, formatMoney(sc.chargeAmount.amount))
        }
    }

    // Tip
    sale.tip?.let { tip ->
        b.twoColumnLine("Tip", formatMoney(tip.amount.amount))
    }

    b.separator('=')

    // Grand total
    b.boldOn().doubleHeight()
    b.twoColumnLine("TOTAL", formatMoney(sale.totalAmount().amount))
    b.normalSize().boldOff()

    b.separator('-')

    // --- Payment info ---
    if (body.showPaymentMethod && sale.payments.isNotEmpty()) {
        for (payment in sale.payments) {
            val methodLabel = when (payment.method) {
                id.stargan.intikasirfnb.domain.transaction.PaymentMethod.CASH -> "Tunai"
                id.stargan.intikasirfnb.domain.transaction.PaymentMethod.CARD -> "Kartu"
                id.stargan.intikasirfnb.domain.transaction.PaymentMethod.E_WALLET -> "E-Wallet"
                id.stargan.intikasirfnb.domain.transaction.PaymentMethod.TRANSFER -> "Transfer"
                id.stargan.intikasirfnb.domain.transaction.PaymentMethod.PLATFORM_GOFOOD -> "GoFood"
                id.stargan.intikasirfnb.domain.transaction.PaymentMethod.PLATFORM_GRABFOOD -> "GrabFood"
                id.stargan.intikasirfnb.domain.transaction.PaymentMethod.PLATFORM_SHOPEEFOOD -> "ShopeeFood"
                id.stargan.intikasirfnb.domain.transaction.PaymentMethod.PLATFORM_OTHER -> "Platform Lain"
                id.stargan.intikasirfnb.domain.transaction.PaymentMethod.OTHER -> "Lainnya"
            }
            b.twoColumnLine(methodLabel, formatMoney(payment.amount.amount))
            payment.reference?.let { ref ->
                if (ref.isNotBlank()) b.line("  Ref: $ref")
            }
        }

        if (sale.changeDue().isPositive()) {
            b.twoColumnLine("Kembali", formatMoney(sale.changeDue().amount))
        }
    }

    // --- Footer ---
    b.feedLines(1)
    b.centerAlign()

    if (footer.showThankYouMessage && footer.thankYouMessage.isNotBlank()) {
        b.boldOn()
        b.line(footer.thankYouMessage)
        b.boldOff()
    }

    footer.customFooterText?.let {
        if (it.isNotBlank()) b.line(it)
    }

    if (footer.showSocialMedia) {
        footer.socialMediaText?.let {
            if (it.isNotBlank()) b.line(it)
        }
    }

    b.feedLines(3)

    // --- Cut ---
    if (printerConfig.autoCut) {
        b.partialCut()
    }

    // --- Cash drawer ---
    if (printerConfig.openCashDrawer) {
        b.openCashDrawer()
    }

    return b.build()
}
