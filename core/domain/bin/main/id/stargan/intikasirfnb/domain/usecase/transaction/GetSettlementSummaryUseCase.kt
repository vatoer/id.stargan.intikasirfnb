package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlementRepository
import id.stargan.intikasirfnb.domain.transaction.SalesChannelId

/**
 * Summary of platform settlement status for reconciliation dashboard.
 */
data class SettlementSummary(
    val pendingCount: Int,
    val totalPending: Money,
    val totalSettled: Money,
    val totalCommission: Money
)

class GetSettlementSummaryUseCase(
    private val settlementRepository: PlatformSettlementRepository
) {
    /**
     * Get settlement summary for an outlet within a date range.
     * pendingCount and totalPending are current (not date-filtered).
     * totalSettled and totalCommission are within the date range.
     */
    suspend operator fun invoke(
        outletId: OutletId,
        fromMillis: Long,
        toMillis: Long
    ): SettlementSummary {
        return SettlementSummary(
            pendingCount = settlementRepository.countPending(outletId),
            totalPending = settlementRepository.totalPendingAmount(outletId),
            totalSettled = settlementRepository.totalSettledAmountInRange(outletId, fromMillis, toMillis),
            totalCommission = settlementRepository.totalCommissionInRange(outletId, fromMillis, toMillis)
        )
    }
}
