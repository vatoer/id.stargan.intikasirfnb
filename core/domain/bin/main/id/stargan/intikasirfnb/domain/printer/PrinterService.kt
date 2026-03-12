package id.stargan.intikasirfnb.domain.printer

interface PrinterService {
    suspend fun print(data: ByteArray): Result<Unit>
    fun disconnect()
}
