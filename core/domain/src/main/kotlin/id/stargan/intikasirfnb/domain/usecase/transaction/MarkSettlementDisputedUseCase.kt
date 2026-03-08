package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.transaction.PlatformSettlement
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlementId
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlementRepository

/**
 * Marks a settlement as disputed when the settled amount doesn't match expectations.
 */
class MarkSettlementDisputedUseCase(
    private val settlementRepository: PlatformSettlementRepository
) {
    suspend operator fun invoke(
        settlementId: PlatformSettlementId,
        notes: String? = null
    ): Result<PlatformSettlement> = runCatching {
        val settlement = settlementRepository.getById(settlementId)
            ?: error("Settlement not found: ${settlementId.value}")
        val updated = settlement.markDisputed(notes)
        settlementRepository.save(updated)
        updated
    }
}
