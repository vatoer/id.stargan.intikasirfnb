package id.stargan.intikasirfnb.data.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import id.stargan.intikasirfnb.domain.printer.PrinterService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID

class BluetoothPrinterService(
    private val bluetoothAdapter: BluetoothAdapter?,
    private val macAddress: String
) : PrinterService {

    private var socket: BluetoothSocket? = null

    @SuppressLint("MissingPermission")
    override suspend fun print(data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val adapter = bluetoothAdapter
                ?: throw IllegalStateException("Bluetooth tidak tersedia di perangkat ini")

            if (!adapter.isEnabled) {
                throw IllegalStateException("Bluetooth belum diaktifkan")
            }

            // Cancel discovery — it slows down connections
            try { adapter.cancelDiscovery() } catch (_: SecurityException) {}

            val device = adapter.getRemoteDevice(macAddress)
                ?: throw IllegalStateException("Perangkat Bluetooth tidak ditemukan: $macAddress")

            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)

            // Connect with timeout
            withTimeout(CONNECT_TIMEOUT_MS) {
                socket!!.connect()
            }

            // Write data
            val outputStream = socket!!.outputStream
            outputStream.write(data)
            outputStream.flush()

            // Wait for Bluetooth transport to finish sending data
            // flush() only flushes the Java buffer, not the BT hardware buffer
            delay(FLUSH_DELAY_MS)
        }.also {
            disconnect()
        }
    }

    override fun disconnect() {
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
    }

    companion object {
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val CONNECT_TIMEOUT_MS = 10000L
        private const val FLUSH_DELAY_MS = 1500L
    }
}
