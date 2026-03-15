package id.stargan.intikasirfnb.domain.printer

import id.stargan.intikasirfnb.domain.settings.OutletProfile
import id.stargan.intikasirfnb.domain.settings.PaperWidth
import id.stargan.intikasirfnb.domain.settings.PrinterConfig
import id.stargan.intikasirfnb.domain.settings.ReceiptConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pre-converted raster image data ready for ESC/POS printing.
 */
data class RasterImage(
    val widthBytes: Int,
    val heightDots: Int,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RasterImage) return false
        return widthBytes == other.widthBytes && heightDots == other.heightDots && data.contentEquals(other.data)
    }
    override fun hashCode(): Int = 31 * (31 * widthBytes + heightDots) + data.contentHashCode()
}

fun buildTestReceipt(
    outletProfile: OutletProfile,
    printerConfig: PrinterConfig,
    receiptConfig: ReceiptConfig,
    logoRaster: RasterImage? = null
): ByteArray {
    val cpl = receiptConfig.paperWidth.defaultCharsPerLine
    val b = EscPosBuilder(cpl)
    val header = receiptConfig.header
    val footer = receiptConfig.footer

    b.initialize()

    // --- Header ---
    b.centerAlign()

    // Logo
    if (header.showLogo && logoRaster != null) {
        b.rasterImage(logoRaster.data, logoRaster.widthBytes, logoRaster.heightDots)
        b.newline()
    }

    // Business name
    if (header.showBusinessName && outletProfile.name.isNotBlank()) {
        b.boldOn().doubleHeight()
        b.line(outletProfile.name)
        b.normalSize().boldOff()
    }

    // Address
    if (header.showAddress) {
        outletProfile.address?.let { b.line(it) }
    }

    // Phone
    if (header.showPhone) {
        outletProfile.phone?.let { b.line("Telp: $it") }
    }

    // NPWP
    if (header.showNpwp) {
        outletProfile.npwp?.let { b.line("NPWP: $it") }
    }

    // Custom header lines
    header.customHeaderLines.forEach { line ->
        if (line.isNotBlank()) b.line(line)
    }

    b.separator('=')

    // --- Test print title ---
    b.boldOn().doubleSize()
    b.line("*** TES CETAK ***")
    b.normalSize().boldOff()

    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.forLanguageTag("id"))
    b.line(dateFormat.format(Date()))

    b.separator('-')

    // --- Sample items ---
    b.leftAlign()
    b.twoColumnLine("Nasi Goreng Spesial", "35.000")
    b.twoColumnLine("Es Teh Manis x2", "16.000")
    b.twoColumnLine("Ayam Bakar", "45.000")
    b.twoColumnLine("Jus Alpukat", "18.000")

    b.separator('-')

    b.boldOn()
    b.twoColumnLine("SUBTOTAL", "114.000")
    b.twoColumnLine("Pajak (11%)", "12.540")
    b.separator('=')
    b.doubleHeight()
    b.twoColumnLine("TOTAL", "126.540")
    b.normalSize().boldOff()

    b.separator('-')

    // --- Footer ---
    b.centerAlign()
    b.feedLines(1)

    // Thank you message
    if (footer.showThankYouMessage && footer.thankYouMessage.isNotBlank()) {
        b.boldOn()
        b.line(footer.thankYouMessage)
        b.boldOff()
    }

    // Custom footer text
    footer.customFooterText?.let {
        if (it.isNotBlank()) b.line(it)
    }

    // Social media
    if (footer.showSocialMedia) {
        footer.socialMediaText?.let {
            if (it.isNotBlank()) b.line(it)
        }
    }

    b.feedLines(1)
    b.line("Printer: ${printerConfig.name ?: "Tidak diketahui"}")
    b.line("Koneksi: ${printerConfig.connectionType.name}")
    b.line("Kertas: ${receiptConfig.paperWidth.mm}mm (${cpl} char/baris)")
    b.feedLines(1)
    b.boldOn()
    b.line("Tes cetak berhasil!")
    b.boldOff()
    b.line("IntiKasir FnB")
    b.feedLines(3)

    // --- Cut ---
    if (printerConfig.autoCut) {
        b.partialCut()
    }

    return b.build()
}

/**
 * Backward-compatible overload (deprecated — use the version with ReceiptConfig).
 */
@Deprecated("Use the overload that accepts ReceiptConfig", ReplaceWith("buildTestReceipt(outletProfile, printerConfig, receiptConfig)"))
fun buildTestReceipt(
    outletProfile: OutletProfile,
    printerConfig: PrinterConfig,
    paperWidth: PaperWidth
): ByteArray = buildTestReceipt(
    outletProfile = outletProfile,
    printerConfig = printerConfig,
    receiptConfig = ReceiptConfig(paperWidth = paperWidth)
)
