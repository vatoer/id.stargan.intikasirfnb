package id.stargan.intikasirfnb.domain.usecase.accounting

import id.stargan.intikasirfnb.domain.accounting.DailySummary
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.transaction.SaleRepository
import id.stargan.intikasirfnb.domain.transaction.SaleStatus
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Get daily sales summary for simple P&L view.
 * Groups completed sales by date and aggregates totals.
 */
class GetDailySummaryUseCase(
    private val saleRepository: SaleRepository
) {
    suspend operator fun invoke(outletId: OutletId, limit: Int = 30): List<DailySummary> {
        val sales = saleRepository.listByOutlet(outletId, limit = 500)
            .filter { it.status == SaleStatus.COMPLETED }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        return sales
            .groupBy { dateFormat.format(java.util.Date(it.createdAtMillis)) }
            .map { (date, daySales) ->
                val totalSales = daySales.fold(Money.zero()) { acc, s -> acc + s.subtotal() }
                val totalTax = daySales.fold(Money.zero()) { acc, s -> acc + s.taxTotal() }
                val totalSC = daySales.fold(Money.zero()) { acc, s -> acc + s.serviceChargeAmount() }
                val totalDiscount = daySales.fold(Money.zero()) { acc, s ->
                    acc + s.lines.fold(Money.zero()) { lineAcc, line -> lineAcc + line.discountAmount }
                }
                DailySummary(
                    date = date,
                    totalSales = totalSales,
                    totalTax = totalTax,
                    totalServiceCharge = totalSC,
                    totalDiscount = totalDiscount,
                    netSales = totalSales - totalDiscount,
                    transactionCount = daySales.size
                )
            }
            .sortedByDescending { it.date }
            .take(limit)
    }
}
