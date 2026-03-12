package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.transaction.SaleRepository
import id.stargan.intikasirfnb.domain.transaction.SaleStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Generates a daily-resetting queue number for counter-service orders.
 * Format: A-001, A-002, ... (resets each day)
 */
class GenerateQueueNumberUseCase(
    private val saleRepository: SaleRepository
) {
    suspend operator fun invoke(outletId: OutletId, prefix: String = "A"): String {
        // Get today's sales to determine next queue number
        val todaySales = saleRepository.listByOutlet(outletId, limit = 999)
        val todayStart = todayStartMillis()
        val todayQueueCount = todaySales.count { sale ->
            sale.createdAtMillis >= todayStart &&
                sale.queueNumber != null &&
                sale.status != SaleStatus.VOIDED
        }
        val nextNumber = todayQueueCount + 1
        return "$prefix-${nextNumber.toString().padStart(3, '0')}"
    }

    private fun todayStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
