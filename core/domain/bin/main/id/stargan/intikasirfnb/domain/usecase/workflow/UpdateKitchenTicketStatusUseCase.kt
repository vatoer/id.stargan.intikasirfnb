package id.stargan.intikasirfnb.domain.usecase.workflow

import id.stargan.intikasirfnb.domain.identity.UserId
import id.stargan.intikasirfnb.domain.shared.DomainEventBus
import id.stargan.intikasirfnb.domain.workflow.KitchenTicket
import id.stargan.intikasirfnb.domain.workflow.KitchenTicketId
import id.stargan.intikasirfnb.domain.workflow.KitchenTicketReady
import id.stargan.intikasirfnb.domain.workflow.KitchenTicketRepository
import id.stargan.intikasirfnb.domain.workflow.KitchenTicketServed
import id.stargan.intikasirfnb.domain.workflow.KitchenTicketStarted
import id.stargan.intikasirfnb.domain.workflow.KitchenTicketStatus

class UpdateKitchenTicketStatusUseCase(
    private val kitchenTicketRepository: KitchenTicketRepository,
    private val eventBus: DomainEventBus? = null
) {
    suspend operator fun invoke(
        ticketId: KitchenTicketId,
        newStatus: KitchenTicketStatus,
        assignedTo: UserId? = null
    ): Result<KitchenTicket> = runCatching {
        val ticket = kitchenTicketRepository.getById(ticketId)
            ?: error("Kitchen ticket not found")

        require(ticket.status.canTransitionTo(newStatus)) {
            "Tidak bisa mengubah status dari ${ticket.status} ke $newStatus"
        }

        val updated = when (newStatus) {
            KitchenTicketStatus.PREPARING -> ticket.startPreparing(assignedTo)
            KitchenTicketStatus.READY -> ticket.markReady()
            KitchenTicketStatus.SERVED -> ticket.markServed()
            KitchenTicketStatus.PENDING -> error("Tidak bisa kembali ke PENDING")
        }

        kitchenTicketRepository.save(updated)

        // Publish domain event based on new status
        when (newStatus) {
            KitchenTicketStatus.PREPARING -> eventBus?.publish(
                KitchenTicketStarted(
                    ticketId = updated.id,
                    saleId = updated.saleId,
                    outletId = updated.outletId,
                    assignedTo = updated.assignedTo,
                    waitTimeMillis = (updated.startedAtMillis ?: 0) - updated.createdAtMillis
                )
            )
            KitchenTicketStatus.READY -> eventBus?.publish(
                KitchenTicketReady(
                    ticketId = updated.id,
                    saleId = updated.saleId,
                    outletId = updated.outletId,
                    prepTimeMillis = updated.prepTimeMillis() ?: 0
                )
            )
            KitchenTicketStatus.SERVED -> eventBus?.publish(
                KitchenTicketServed(
                    ticketId = updated.id,
                    saleId = updated.saleId,
                    outletId = updated.outletId,
                    totalTimeMillis = (updated.servedAtMillis ?: 0) - updated.createdAtMillis
                )
            )
            else -> {} // no event for PENDING
        }

        updated
    }
}
