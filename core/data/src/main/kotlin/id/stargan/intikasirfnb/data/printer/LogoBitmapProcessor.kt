package id.stargan.intikasirfnb.data.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import dagger.hilt.android.qualifiers.ApplicationContext
import id.stargan.intikasirfnb.domain.printer.RasterImage
import id.stargan.intikasirfnb.domain.settings.LogoSize
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Converts logo images to monochrome raster data for ESC/POS thermal printers.
 * Caches the raster output so it doesn't need re-conversion on every print.
 */
@Singleton
class LogoBitmapProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Convert and cache a logo image for thermal printing.
     * Call this when the user uploads/changes a logo.
     *
     * @param imagePath absolute path to the source image file
     * @param logoSize desired print size
     * @param paperWidthDots total print width in dots (e.g., 384 for 58mm, 576 for 80mm)
     * @return path to the cached raster file, or null on failure
     */
    fun processAndCache(
        imagePath: String,
        logoSize: LogoSize = LogoSize.MEDIUM,
        paperWidthDots: Int = 384
    ): String? {
        return try {
            val sourceFile = File(imagePath)
            if (!sourceFile.exists()) return null

            val maxWidthDots = when (logoSize) {
                LogoSize.SMALL -> paperWidthDots / 4
                LogoSize.MEDIUM -> paperWidthDots / 2
                LogoSize.LARGE -> (paperWidthDots * 3) / 4
            }

            val original = BitmapFactory.decodeFile(imagePath) ?: return null
            val scaled = scaleToWidth(original, maxWidthDots)
            val raster = bitmapToRaster(scaled)

            // Save raster cache file
            val cacheFile = getRasterCacheFile(imagePath)
            cacheFile.parentFile?.mkdirs()
            cacheFile.outputStream().use { out ->
                val widthBytes = (scaled.width + 7) / 8
                val heightDots = scaled.height
                // Simple header: 4 bytes widthBytes + 4 bytes heightDots + raw data
                out.write(intToBytes(widthBytes))
                out.write(intToBytes(heightDots))
                out.write(raster)
            }

            if (original !== scaled) scaled.recycle()
            original.recycle()

            cacheFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Load cached raster data for printing.
     * @param imagePath the original logo image path (cache path is derived from it)
     * @return RasterImage ready for EscPosBuilder, or null if no cache exists
     */
    fun loadCachedRaster(imagePath: String): RasterImage? {
        return try {
            val cacheFile = getRasterCacheFile(imagePath)
            if (!cacheFile.exists()) return null

            val bytes = cacheFile.readBytes()
            if (bytes.size < 8) return null

            val widthBytes = bytesToInt(bytes, 0)
            val heightDots = bytesToInt(bytes, 4)
            val data = bytes.copyOfRange(8, bytes.size)

            if (data.size != widthBytes * heightDots) return null

            RasterImage(widthBytes, heightDots, data)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Delete cached raster for a given logo path.
     */
    fun deleteCachedRaster(imagePath: String) {
        getRasterCacheFile(imagePath).delete()
    }

    private fun getRasterCacheFile(imagePath: String): File {
        val dir = File(context.filesDir, "logo_raster_cache")
        val hash = imagePath.hashCode().toUInt().toString(16)
        return File(dir, "raster_$hash.bin")
    }

    private fun scaleToWidth(bitmap: Bitmap, maxWidth: Int): Bitmap {
        if (bitmap.width <= maxWidth) return bitmap
        val ratio = maxWidth.toFloat() / bitmap.width
        val newHeight = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
    }

    /**
     * Convert a Bitmap to monochrome 1-bit raster data.
     * Uses Floyd-Steinberg dithering for better thermal print quality.
     */
    private fun bitmapToRaster(bitmap: Bitmap): ByteArray {
        val w = bitmap.width
        val h = bitmap.height
        val widthBytes = (w + 7) / 8

        // Get grayscale error diffusion buffer
        val gray = FloatArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                gray[y * w + x] = 0.299f * r + 0.587f * g + 0.114f * b
            }
        }

        // Floyd-Steinberg dithering
        val result = ByteArray(widthBytes * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                val oldVal = gray[idx]
                val newVal = if (oldVal < 128f) 0f else 255f
                val error = oldVal - newVal

                // Black pixel = 1 in ESC/POS raster
                if (newVal == 0f) {
                    val byteIndex = y * widthBytes + (x / 8)
                    val bitIndex = 7 - (x % 8)
                    result[byteIndex] = (result[byteIndex].toInt() or (1 shl bitIndex)).toByte()
                }

                // Distribute error
                if (x + 1 < w) gray[idx + 1] += error * 7f / 16f
                if (y + 1 < h) {
                    if (x > 0) gray[idx + w - 1] += error * 3f / 16f
                    gray[idx + w] += error * 5f / 16f
                    if (x + 1 < w) gray[idx + w + 1] += error * 1f / 16f
                }
            }
        }

        return result
    }

    private fun intToBytes(value: Int): ByteArray = byteArrayOf(
        (value shr 24).toByte(),
        (value shr 16).toByte(),
        (value shr 8).toByte(),
        value.toByte()
    )

    private fun bytesToInt(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF shl 24) or
        (bytes[offset + 1].toInt() and 0xFF shl 16) or
        (bytes[offset + 2].toInt() and 0xFF shl 8) or
        (bytes[offset + 3].toInt() and 0xFF)
}
