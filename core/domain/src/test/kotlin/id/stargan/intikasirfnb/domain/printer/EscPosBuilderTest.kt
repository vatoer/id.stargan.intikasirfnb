package id.stargan.intikasirfnb.domain.printer

import id.stargan.intikasirfnb.domain.settings.OutletProfile
import id.stargan.intikasirfnb.domain.settings.PaperWidth
import id.stargan.intikasirfnb.domain.settings.PrinterConfig
import id.stargan.intikasirfnb.domain.settings.PrinterConnectionType
import id.stargan.intikasirfnb.domain.settings.ReceiptConfig
import id.stargan.intikasirfnb.domain.settings.ReceiptFooterConfig
import id.stargan.intikasirfnb.domain.settings.ReceiptHeaderConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EscPosBuilderTest {

    @Test
    fun `initialize sends ESC @ command`() {
        val bytes = EscPosBuilder().initialize().build()
        assertEquals(0x1B.toByte(), bytes[0])
        assertEquals(0x40.toByte(), bytes[1])
    }

    @Test
    fun `centerAlign sends correct bytes`() {
        val bytes = EscPosBuilder().centerAlign().build()
        assertEquals(0x1B.toByte(), bytes[0])
        assertEquals(0x61.toByte(), bytes[1])
        assertEquals(0x01.toByte(), bytes[2])
    }

    @Test
    fun `boldOn and boldOff toggle correctly`() {
        val bytes = EscPosBuilder().boldOn().boldOff().build()
        // boldOn: 1B 45 01
        assertEquals(0x1B.toByte(), bytes[0])
        assertEquals(0x45.toByte(), bytes[1])
        assertEquals(0x01.toByte(), bytes[2])
        // boldOff: 1B 45 00
        assertEquals(0x1B.toByte(), bytes[3])
        assertEquals(0x45.toByte(), bytes[4])
        assertEquals(0x00.toByte(), bytes[5])
    }

    @Test
    fun `text outputs ISO-8859-1 encoded bytes`() {
        val bytes = EscPosBuilder().text("ABC").build()
        assertEquals(3, bytes.size)
        assertEquals('A'.code.toByte(), bytes[0])
        assertEquals('B'.code.toByte(), bytes[1])
        assertEquals('C'.code.toByte(), bytes[2])
    }

    @Test
    fun `separator fills full line`() {
        val cpl = 32
        val bytes = EscPosBuilder(cpl).separator('-').build()
        val text = String(bytes, Charsets.ISO_8859_1)
        assertTrue(text.startsWith("-".repeat(cpl)))
    }

    @Test
    fun `twoColumnLine pads correctly`() {
        val cpl = 32
        val bytes = EscPosBuilder(cpl).twoColumnLine("Item", "100").build()
        val text = String(bytes, Charsets.ISO_8859_1).trimEnd('\n')
        assertEquals(cpl, text.length)
        assertTrue(text.startsWith("Item"))
        assertTrue(text.endsWith("100"))
    }

    @Test
    fun `fullCut sends correct bytes`() {
        val bytes = EscPosBuilder().fullCut().build()
        assertEquals(0x1D.toByte(), bytes[0])
        assertEquals(0x56.toByte(), bytes[1])
        assertEquals(0x00.toByte(), bytes[2])
    }

    @Test
    fun `partialCut sends correct bytes`() {
        val bytes = EscPosBuilder().partialCut().build()
        assertEquals(0x1D.toByte(), bytes[0])
        assertEquals(0x56.toByte(), bytes[1])
        assertEquals(0x01.toByte(), bytes[2])
    }

    @Test
    fun `openCashDrawer sends correct bytes`() {
        val bytes = EscPosBuilder().openCashDrawer().build()
        assertEquals(0x1B.toByte(), bytes[0])
        assertEquals(0x70.toByte(), bytes[1])
    }

    @Test
    fun `feedLines sends correct command`() {
        val bytes = EscPosBuilder().feedLines(3).build()
        assertEquals(0x1B.toByte(), bytes[0])
        assertEquals(0x64.toByte(), bytes[1])
        assertEquals(3.toByte(), bytes[2])
    }

    // --- TestReceiptBuilder ---

    @Test
    fun `buildTestReceipt produces non-empty bytes`() {
        val receipt = buildTestReceipt(
            outletProfile = OutletProfile(name = "Warung Test", address = "Jl. Test"),
            printerConfig = PrinterConfig(
                connectionType = PrinterConnectionType.BLUETOOTH,
                name = "Test Printer",
                autoCut = true
            ),
            receiptConfig = ReceiptConfig(paperWidth = PaperWidth.THERMAL_58MM)
        )
        assertTrue(receipt.isNotEmpty())
        // Should start with ESC @ (initialize)
        assertEquals(0x1B.toByte(), receipt[0])
        assertEquals(0x40.toByte(), receipt[1])
    }

    @Test
    fun `buildTestReceipt contains store name`() {
        val receipt = buildTestReceipt(
            outletProfile = OutletProfile(name = "Kafe Merdeka"),
            printerConfig = PrinterConfig(connectionType = PrinterConnectionType.NETWORK),
            receiptConfig = ReceiptConfig(paperWidth = PaperWidth.THERMAL_80MM)
        )
        val text = String(receipt, Charsets.ISO_8859_1)
        assertTrue(text.contains("Kafe Merdeka"))
        assertTrue(text.contains("TES CETAK"))
    }

    @Test
    fun `buildTestReceipt uses 80mm char width`() {
        val receipt = buildTestReceipt(
            outletProfile = OutletProfile(name = "Test"),
            printerConfig = PrinterConfig(connectionType = PrinterConnectionType.NETWORK),
            receiptConfig = ReceiptConfig(paperWidth = PaperWidth.THERMAL_80MM)
        )
        val text = String(receipt, Charsets.ISO_8859_1)
        // 80mm separator should be 48 chars
        assertTrue(text.contains("=".repeat(48)))
    }

    @Test
    fun `buildTestReceipt includes partial cut when autoCut is true`() {
        val receipt = buildTestReceipt(
            outletProfile = OutletProfile(name = "Test"),
            printerConfig = PrinterConfig(autoCut = true),
            receiptConfig = ReceiptConfig()
        )
        // Partial cut bytes: 1D 56 01
        val lastBytes = receipt.takeLast(3)
        assertEquals(0x1D.toByte(), lastBytes[0])
        assertEquals(0x56.toByte(), lastBytes[1])
        assertEquals(0x01.toByte(), lastBytes[2])
    }

    @Test
    fun `buildTestReceipt no cut when autoCut is false`() {
        val receipt = buildTestReceipt(
            outletProfile = OutletProfile(name = "Test"),
            printerConfig = PrinterConfig(autoCut = false),
            receiptConfig = ReceiptConfig()
        )
        // Should NOT end with cut command
        val lastBytes = receipt.takeLast(3)
        val isCutCommand = lastBytes[0] == 0x1D.toByte() && lastBytes[1] == 0x56.toByte()
        assertTrue(!isCutCommand)
    }

    // --- rasterImage ---

    @Test
    fun `rasterImage sends GS v 0 header then data`() {
        val data = byteArrayOf(0xFF.toByte(), 0x00) // 2 bytes of raster data
        val bytes = EscPosBuilder().rasterImage(data, widthBytes = 1, heightDots = 2).build()
        // GS v 0 m xL xH yL yH d1...dk
        assertEquals(0x1D.toByte(), bytes[0])
        assertEquals(0x76.toByte(), bytes[1])
        assertEquals(0x30.toByte(), bytes[2])
        assertEquals(0x00.toByte(), bytes[3]) // m = normal
        assertEquals(0x01.toByte(), bytes[4]) // xL = 1
        assertEquals(0x00.toByte(), bytes[5]) // xH = 0
        assertEquals(0x02.toByte(), bytes[6]) // yL = 2
        assertEquals(0x00.toByte(), bytes[7]) // yH = 0
        // data follows
        assertEquals(0xFF.toByte(), bytes[8])
        assertEquals(0x00.toByte(), bytes[9])
        assertEquals(10, bytes.size)
    }

    @Test
    fun `rasterImage handles large dimensions`() {
        val widthBytes = 48 // 384 dots / 8
        val heightDots = 300
        val data = ByteArray(widthBytes * heightDots)
        val bytes = EscPosBuilder().rasterImage(data, widthBytes, heightDots).build()
        // Header is 8 bytes
        assertEquals(8 + data.size, bytes.size)
        // xL = 48, xH = 0
        assertEquals(48.toByte(), bytes[4])
        assertEquals(0x00.toByte(), bytes[5])
        // yL = 300 & 0xFF = 44, yH = 300 >> 8 = 1
        assertEquals(44.toByte(), bytes[6])
        assertEquals(1.toByte(), bytes[7])
    }

    // --- buildTestReceipt with ReceiptConfig ---

    @Test
    fun `buildTestReceipt with ReceiptConfig respects showBusinessName false`() {
        val receipt = buildTestReceipt(
            outletProfile = OutletProfile(name = "Hidden Store"),
            printerConfig = PrinterConfig(),
            receiptConfig = ReceiptConfig(
                header = ReceiptHeaderConfig(showBusinessName = false)
            )
        )
        val text = String(receipt, Charsets.ISO_8859_1)
        assertFalse(text.contains("Hidden Store"))
        assertTrue(text.contains("TES CETAK"))
    }

    @Test
    fun `buildTestReceipt with ReceiptConfig includes NPWP when enabled`() {
        val receipt = buildTestReceipt(
            outletProfile = OutletProfile(name = "Toko", npwp = "12.345.678.9-012.345"),
            printerConfig = PrinterConfig(),
            receiptConfig = ReceiptConfig(
                header = ReceiptHeaderConfig(showNpwp = true)
            )
        )
        val text = String(receipt, Charsets.ISO_8859_1)
        assertTrue(text.contains("NPWP: 12.345.678.9-012.345"))
    }

    @Test
    fun `buildTestReceipt with ReceiptConfig hides NPWP by default`() {
        val receipt = buildTestReceipt(
            outletProfile = OutletProfile(name = "Toko", npwp = "12.345.678.9-012.345"),
            printerConfig = PrinterConfig(),
            receiptConfig = ReceiptConfig()
        )
        val text = String(receipt, Charsets.ISO_8859_1)
        assertFalse(text.contains("NPWP"))
    }

    @Test
    fun `buildTestReceipt with ReceiptConfig hides address when disabled`() {
        val receipt = buildTestReceipt(
            outletProfile = OutletProfile(name = "Toko", address = "Jl. Secret"),
            printerConfig = PrinterConfig(),
            receiptConfig = ReceiptConfig(
                header = ReceiptHeaderConfig(showAddress = false)
            )
        )
        val text = String(receipt, Charsets.ISO_8859_1)
        assertFalse(text.contains("Jl. Secret"))
    }

    @Test
    fun `buildTestReceipt with ReceiptConfig hides phone when disabled`() {
        val receipt = buildTestReceipt(
            outletProfile = OutletProfile(name = "Toko", phone = "021-9999"),
            printerConfig = PrinterConfig(),
            receiptConfig = ReceiptConfig(
                header = ReceiptHeaderConfig(showPhone = false)
            )
        )
        val text = String(receipt, Charsets.ISO_8859_1)
        assertFalse(text.contains("021-9999"))
    }

    @Test
    fun `buildTestReceipt with ReceiptConfig includes thank you message`() {
        val receipt = buildTestReceipt(
            outletProfile = OutletProfile(name = "Toko"),
            printerConfig = PrinterConfig(),
            receiptConfig = ReceiptConfig(
                footer = ReceiptFooterConfig(
                    showThankYouMessage = true,
                    thankYouMessage = "Terima kasih sudah mampir!"
                )
            )
        )
        val text = String(receipt, Charsets.ISO_8859_1)
        assertTrue(text.contains("Terima kasih sudah mampir!"))
    }

    @Test
    fun `buildTestReceipt with ReceiptConfig hides thank you when disabled`() {
        val receipt = buildTestReceipt(
            outletProfile = OutletProfile(name = "Toko"),
            printerConfig = PrinterConfig(),
            receiptConfig = ReceiptConfig(
                footer = ReceiptFooterConfig(showThankYouMessage = false)
            )
        )
        val text = String(receipt, Charsets.ISO_8859_1)
        assertFalse(text.contains("Terima kasih"))
    }

    @Test
    fun `buildTestReceipt with ReceiptConfig includes social media`() {
        val receipt = buildTestReceipt(
            outletProfile = OutletProfile(name = "Toko"),
            printerConfig = PrinterConfig(),
            receiptConfig = ReceiptConfig(
                footer = ReceiptFooterConfig(
                    showSocialMedia = true,
                    socialMediaText = "IG: @tokoku"
                )
            )
        )
        val text = String(receipt, Charsets.ISO_8859_1)
        assertTrue(text.contains("IG: @tokoku"))
    }

    @Test
    fun `buildTestReceipt with ReceiptConfig hides social media by default`() {
        val receipt = buildTestReceipt(
            outletProfile = OutletProfile(name = "Toko"),
            printerConfig = PrinterConfig(),
            receiptConfig = ReceiptConfig(
                footer = ReceiptFooterConfig(
                    showSocialMedia = false,
                    socialMediaText = "IG: @tokoku"
                )
            )
        )
        val text = String(receipt, Charsets.ISO_8859_1)
        assertFalse(text.contains("IG: @tokoku"))
    }

    @Test
    fun `buildTestReceipt with ReceiptConfig includes custom footer`() {
        val receipt = buildTestReceipt(
            outletProfile = OutletProfile(name = "Toko"),
            printerConfig = PrinterConfig(),
            receiptConfig = ReceiptConfig(
                footer = ReceiptFooterConfig(
                    customFooterText = "Promo: beli 2 gratis 1"
                )
            )
        )
        val text = String(receipt, Charsets.ISO_8859_1)
        assertTrue(text.contains("Promo: beli 2 gratis 1"))
    }

    @Test
    fun `buildTestReceipt with ReceiptConfig includes custom header lines`() {
        val receipt = buildTestReceipt(
            outletProfile = OutletProfile(name = "Toko"),
            printerConfig = PrinterConfig(),
            receiptConfig = ReceiptConfig(
                header = ReceiptHeaderConfig(
                    customHeaderLines = listOf("Cabang Jakarta Pusat", "Lantai 2")
                )
            )
        )
        val text = String(receipt, Charsets.ISO_8859_1)
        assertTrue(text.contains("Cabang Jakarta Pusat"))
        assertTrue(text.contains("Lantai 2"))
    }

    @Test
    fun `buildTestReceipt with logo raster includes raster command`() {
        val rasterData = byteArrayOf(0xFF.toByte(), 0x00, 0xAA.toByte(), 0x55)
        val logo = RasterImage(widthBytes = 2, heightDots = 2, data = rasterData)

        val receipt = buildTestReceipt(
            outletProfile = OutletProfile(name = "Toko"),
            printerConfig = PrinterConfig(),
            receiptConfig = ReceiptConfig(
                header = ReceiptHeaderConfig(showLogo = true)
            ),
            logoRaster = logo
        )
        // Should contain GS v 0 command bytes
        val bytes = receipt
        var foundRaster = false
        for (i in 0 until bytes.size - 7) {
            if (bytes[i] == 0x1D.toByte() && bytes[i + 1] == 0x76.toByte() && bytes[i + 2] == 0x30.toByte()) {
                foundRaster = true
                break
            }
        }
        assertTrue("Receipt should contain raster image command", foundRaster)
    }

    @Test
    fun `buildTestReceipt without logo does not include raster command`() {
        val receipt = buildTestReceipt(
            outletProfile = OutletProfile(name = "Toko"),
            printerConfig = PrinterConfig(),
            receiptConfig = ReceiptConfig(
                header = ReceiptHeaderConfig(showLogo = true)
            ),
            logoRaster = null
        )
        val bytes = receipt
        var foundRaster = false
        for (i in 0 until bytes.size - 2) {
            if (bytes[i] == 0x1D.toByte() && bytes[i + 1] == 0x76.toByte() && bytes[i + 2] == 0x30.toByte()) {
                foundRaster = true
                break
            }
        }
        assertFalse("Receipt should not contain raster command without logo", foundRaster)
    }
}
