package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlement
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlementRepository
import id.stargan.intikasirfnb.domain.transaction.SalesChannelId

/**
 * Retrieves pending (unsettled) platform settlements.
 * Can filter by outlet or narrow down to a specific channel.
 */
class GetPendingSettlementsUseCase(
    private val settlementRepository: PlatformSettlementRepository
) {
    suspend operator fun invoke(
        outletId: OutletId,
        channelId: SalesChannelId? = null
    ): List<PlatformSettlement> {
        return if (channelId != null) {
            settlementRepository.listPendingByChannel(channelId)
        } else {
            settlementRepository.listPending(outletId)
        }
    }
}
