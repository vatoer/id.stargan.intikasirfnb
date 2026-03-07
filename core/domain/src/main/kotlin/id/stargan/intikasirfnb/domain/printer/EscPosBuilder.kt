package id.stargan.intikasirfnb.domain.printer

import java.io.ByteArrayOutputStream

/**
 * Pure Kotlin ESC/POS command builder for thermal printers.
 * Generates raw byte arrays — no Android dependencies.
 */
class EscPosBuilder(private val charsPerLine: Int = 32) {

    private val buffer = ByteArrayOutputStream()
    private val charset = Charsets.ISO_8859_1

    fun initialize(): EscPosBuilder = apply {
        buffer.write(byteArrayOf(0x1B, 0x40)) // ESC @
    }

    // --- Alignment ---

    fun leftAlign(): EscPosBuilder = apply {
        buffer.write(byteArrayOf(0x1B, 0x61, 0x00))
    }

    fun centerAlign(): EscPosBuilder = apply {
        buffer.write(byteArrayOf(0x1B, 0x61, 0x01))
    }

    fun rightAlign(): EscPosBuilder = apply {
        buffer.write(byteArrayOf(0x1B, 0x61, 0x02))
    }

    // --- Text style ---

    fun boldOn(): EscPosBuilder = apply {
        buffer.write(byteArrayOf(0x1B, 0x45, 0x01))
    }

    fun boldOff(): EscPosBuilder = apply {
        buffer.write(byteArrayOf(0x1B, 0x45, 0x00))
    }

    fun doubleHeight(): EscPosBuilder = apply {
        buffer.write(byteArrayOf(0x1B, 0x21, 0x10))
    }

    fun doubleWidth(): EscPosBuilder = apply {
        buffer.write(byteArrayOf(0x1B, 0x21, 0x20))
    }

    fun doubleSize(): EscPosBuilder = apply {
        buffer.write(byteArrayOf(0x1B, 0x21, 0x30))
    }

    fun normalSize(): EscPosBuilder = apply {
        buffer.write(byteArrayOf(0x1B, 0x21, 0x00))
    }

    // --- Text output ---

    fun text(s: String): EscPosBuilder = apply {
        buffer.write(s.toByteArray(charset))
    }

    fun newline(): EscPosBuilder = apply {
        buffer.write("\n".toByteArray(charset))
    }

    fun line(s: String): EscPosBuilder = text(s).newline()

    fun feedLines(n: Int): EscPosBuilder = apply {
        buffer.write(byteArrayOf(0x1B, 0x64, n.coerceIn(0, 255).toByte()))
    }

    // --- Layout helpers ---

    fun separator(char: Char = '-'): EscPosBuilder =
        line(char.toString().repeat(charsPerLine))

    fun twoColumnLine(left: String, right: String): EscPosBuilder {
        val space = charsPerLine - left.length - right.length
        return if (space >= 1) {
            line(left + " ".repeat(space) + right)
        } else {
            line(left)
            rightAlign()
            line(right)
            leftAlign()
        }
    }

    fun threeColumnLine(left: String, center: String, right: String): EscPosBuilder {
        val totalContent = left.length + center.length + right.length
        val totalSpace = charsPerLine - totalContent
        if (totalSpace < 2) return line("$left $center $right")
        val leftSpace = totalSpace / 2
        val rightSpace = totalSpace - leftSpace
        return line(left + " ".repeat(leftSpace) + center + " ".repeat(rightSpace) + right)
    }

    // --- Paper control ---

    fun fullCut(): EscPosBuilder = apply {
        buffer.write(byteArrayOf(0x1D, 0x56, 0x00))
    }

    fun partialCut(): EscPosBuilder = apply {
        buffer.write(byteArrayOf(0x1D, 0x56, 0x01))
    }

    // --- Image (raster bitmap) ---

    /**
     * Print a raster bitmap using GS v 0.
     * @param data monochrome raster data (1 bit per pixel, MSB=left, 1=black)
     * @param widthBytes width in bytes (= widthDots / 8)
     * @param heightDots height in dots (pixels)
     */
    fun rasterImage(data: ByteArray, widthBytes: Int, heightDots: Int): EscPosBuilder = apply {
        val xL = (widthBytes and 0xFF).toByte()
        val xH = ((widthBytes shr 8) and 0xFF).toByte()
        val yL = (heightDots and 0xFF).toByte()
        val yH = ((heightDots shr 8) and 0xFF).toByte()
        // GS v 0 m xL xH yL yH d1...dk
        buffer.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH))
        buffer.write(data)
    }

    // --- Hardware ---

    fun openCashDrawer(): EscPosBuilder = apply {
        buffer.write(byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte()))
    }

    fun build(): ByteArray = buffer.toByteArray()
}
