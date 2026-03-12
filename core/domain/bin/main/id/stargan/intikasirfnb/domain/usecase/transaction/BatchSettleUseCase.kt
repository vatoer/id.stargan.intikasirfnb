package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlement
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlementId
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlementRepository

/**
 * Result of a batch settlement operation.
 */
data class BatchSettleResult(
    val settled: List<PlatformSettlement>,
    val totalExpected: Money,
    val totalSettled: Money,
    val hasDiscrepancy: Boolean
)

/**
 * Batch-settles multiple pending settlements at once (e.g. when platform
 * transfers a lump sum covering multiple orders).
 *
 * Each settlement is marked as settled with its expected amount.
 * If the actual total differs from expected total, the last settlement
 * absorbs the difference (marked as PARTIAL if underpaid).
 */
class BatchSettleUseCase(
    private val settlementRepository: PlatformSettlementRepository
) {
    suspend operator fun invoke(
        settlementIds: List<PlatformSettlementId>,
        totalReceivedAmount: Money,
        platformReference: String? = null,
        settlementDate: Long = System.currentTimeMillis()
    ): Result<BatchSettleResult> = runCatching {
        require(settlementIds.isNotEmpty()) { "No settlements to settle" }

        val settlements = settlementIds.map { id ->
            settlementRepository.getById(id) ?: error("Settlement not found: ${id.value}")
        }

        val totalExpected = settlements.fold(Money.zero()) { acc, s -> acc + s.expectedAmount }
        val hasDiscrepancy = totalReceivedAmount.amount.compareTo(totalExpected.amount) != 0

        val settled = if (!hasDiscrepancy) {
            // Exact match — settle each at expected amount
            settlements.map { s ->
                val updated = s.markSettled(s.expectedAmount, platformReference, settlementDate)
                settlementRepository.save(updated)
                updated
            }
        } else {
            // Distribute: first N-1 get expected amount, last one gets remainder
            val allButLast = settlements.dropLast(1)
            val last = settlements.last()
            val settledSoFar = allButLast.fold(Money.zero()) { acc, s -> acc + s.expectedAmount }
            val remainder = Money(
                totalReceivedAmount.amount.subtract(settledSoFar.amount),
                totalReceivedAmount.currencyCode
            )

            val results = allButLast.map { s ->
                val updated = s.markSettled(s.expectedAmount, platformReference, settlementDate)
                settlementRepository.save(updated)
                updated
            }
            val lastUpdated = last.markSettled(remainder, platformReference, settlementDate)
            settlementRepository.save(lastUpdated)
            results + lastUpdated
        }

        BatchSettleResult(
            settled = settled,
            totalExpected = totalExpected,
            totalSettled = totalReceivedAmount,
            hasDiscrepancy = hasDiscrepancy
        )
    }
}
