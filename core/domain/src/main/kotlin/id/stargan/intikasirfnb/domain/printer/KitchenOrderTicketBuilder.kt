package id.stargan.intikasirfnb.domain.printer

import id.stargan.intikasirfnb.domain.settings.PrinterConfig
import id.stargan.intikasirfnb.domain.transaction.OrderLine
import id.stargan.intikasirfnb.domain.transaction.Sale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds a Kitchen Order Ticket (KOT) for thermal printer.
 * Only prints the delta (unsent) items, not the full order.
 */
fun buildKitchenOrderTicket(
    sale: Sale,
    newLines: List<OrderLine>,
    printerConfig: PrinterConfig,
    tableName: String? = null,
    channelName: String? = null,
    cashierName: String? = null,
    ticketNumber: Int = 1
): ByteArray {
    val cpl = 32 // Standard 58mm paper
    val b = EscPosBuilder(cpl)
    val dateFormat = SimpleDateFormat("HH:mm", Locale.forLanguageTag("id"))

    b.initialize()
    b.centerAlign()

    // Header
    b.boldOn().doubleHeight()
    b.line("** KOT **")
    b.normalSize().boldOff()

    if (ticketNumber > 1) {
        b.line("Tambahan #$ticketNumber")
    }

    b.separator('=')

    // Order info
    b.leftAlign()
    sale.receiptNumber?.let { b.twoColumnLine("No.", it) }
    b.twoColumnLine("Waktu", dateFormat.format(Date()))
    tableName?.let { b.twoColumnLine("Meja", it) }
    channelName?.let { b.twoColumnLine("Channel", it) }
    cashierName?.let { b.twoColumnLine("Kasir", it) }

    b.separator('-')

    // Items (only new/delta items)
    b.boldOn()
    for (line in newLines) {
        b.line("${line.quantity}x ${line.productRef.name}")
        if (line.selectedModifiers.isNotEmpty()) {
            val modText = line.selectedModifiers.joinToString(", ") { it.optionName }
            b.line("  ($modText)")
        }
        if (line.selectedAddOns.isNotEmpty()) {
            line.selectedAddOns.forEach { addOn ->
                b.line("  + ${addOn.addOnName} x${addOn.quantity}")
            }
        }
        if (!line.notes.isNullOrBlank()) {
            b.line("  * ${line.notes}")
        }
    }
    b.boldOff()

    b.separator('=')

    // Summary
    b.centerAlign()
    b.line("Total: ${newLines.sumOf { it.quantity }} item")

    b.feedLines(3)

    if (printerConfig.autoCut) {
        b.partialCut()
    }

    return b.build()
}
