package id.stargan.intikasirfnb.domain.usecase.workflow

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.workflow.KitchenTicket
import id.stargan.intikasirfnb.domain.workflow.KitchenTicketRepository
import kotlinx.coroutines.flow.Flow

class GetActiveKitchenTicketsUseCase(
    private val kitchenTicketRepository: KitchenTicketRepository
) {
    /** Get active tickets (PENDING, PREPARING, READY) as a reactive Flow */
    fun stream(outletId: OutletId): Flow<List<KitchenTicket>> =
        kitchenTicketRepository.streamActiveByOutlet(outletId)

    /** Get active tickets (one-shot) */
    suspend operator fun invoke(outletId: OutletId): List<KitchenTicket> =
        kitchenTicketRepository.getActiveByOutlet(outletId)
}
