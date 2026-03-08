package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlement
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlementId
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlementRepository

/**
 * Marks a pending settlement as settled with the actual amount received from the platform.
 * Automatically determines SETTLED vs PARTIAL based on expected vs actual amount.
 */
class MarkSettlementSettledUseCase(
    private val settlementRepository: PlatformSettlementRepository
) {
    suspend operator fun invoke(
        settlementId: PlatformSettlementId,
        settledAmount: Money,
        platformReference: String? = null,
        settlementDate: Long = System.currentTimeMillis()
    ): Result<PlatformSettlement> = runCatching {
        val settlement = settlementRepository.getById(settlementId)
            ?: error("Settlement not found: ${settlementId.value}")
        val updated = settlement.markSettled(settledAmount, platformReference, settlementDate)
        settlementRepository.save(updated)
        updated
    }
}
