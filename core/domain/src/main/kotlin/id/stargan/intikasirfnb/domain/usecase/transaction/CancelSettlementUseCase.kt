package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.transaction.PlatformSettlement
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlementId
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlementRepository

/**
 * Cancels a pending settlement (e.g. when the platform order is cancelled/refunded).
 */
class CancelSettlementUseCase(
    private val settlementRepository: PlatformSettlementRepository
) {
    suspend operator fun invoke(settlementId: PlatformSettlementId): Result<PlatformSettlement> = runCatching {
        val settlement = settlementRepository.getById(settlementId)
            ?: error("Settlement not found: ${settlementId.value}")
        val updated = settlement.markCancelled()
        settlementRepository.save(updated)
        updated
    }
}
