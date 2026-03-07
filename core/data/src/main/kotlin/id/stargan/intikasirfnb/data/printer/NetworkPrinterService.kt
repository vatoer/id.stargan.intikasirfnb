package id.stargan.intikasirfnb.data.printer

import id.stargan.intikasirfnb.domain.printer.PrinterService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Socket

class NetworkPrinterService(
    private val host: String,
    private val port: Int = 9100
) : PrinterService {

    override suspend fun print(data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            Socket(host, port).use { socket ->
                socket.soTimeout = 5000
                socket.getOutputStream().write(data)
                socket.getOutputStream().flush()
            }
        }
    }

    override fun disconnect() {
        // Network connections are opened/closed per print job
    }
}
