package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlement
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlementId
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlementRepository
import id.stargan.intikasirfnb.domain.transaction.Sale

/**
 * Creates a PlatformSettlement record from a completed platform sale.
 * Called automatically when a platform order is completed (sale.platformPayment != null).
 * Each sale creates one settlement record (1:1). Batch settlements can be
 * created separately via [CreateBatchSettlementUseCase].
 */
class CreatePlatformSettlementUseCase(
    private val settlementRepository: PlatformSettlementRepository
) {
    suspend operator fun invoke(sale: Sale): Result<PlatformSettlement> = runCatching {
        val pp = sale.platformPayment
            ?: error("Sale ${sale.id.value} has no platform payment — not a platform order")

        val settlement = PlatformSettlement(
            id = PlatformSettlementId.generate(),
            outletId = sale.outletId,
            channelId = sale.channelId,
            platformName = pp.platformName,
            saleIds = listOf(sale.id),
            expectedAmount = pp.netAmount,
            commissionTotal = pp.commissionAmount
        )

        settlementRepository.save(settlement)
        settlement
    }
}
