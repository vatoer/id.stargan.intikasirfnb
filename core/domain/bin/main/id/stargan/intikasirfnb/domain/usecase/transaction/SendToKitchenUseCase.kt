package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.shared.DomainEventBus
import id.stargan.intikasirfnb.domain.transaction.OrderLine
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleRepository
import id.stargan.intikasirfnb.domain.workflow.KitchenTicket
import id.stargan.intikasirfnb.domain.workflow.KitchenTicketCreated
import id.stargan.intikasirfnb.domain.workflow.KitchenTicketId
import id.stargan.intikasirfnb.domain.workflow.KitchenTicketItem
import id.stargan.intikasirfnb.domain.workflow.KitchenTicketRepository

data class KitchenTicketResult(
    val sale: Sale,
    val newLines: List<OrderLine>,
    val ticket: KitchenTicket? = null
)

class SendToKitchenUseCase(
    private val saleRepository: SaleRepository,
    private val kitchenTicketRepository: KitchenTicketRepository,
    private val eventBus: DomainEventBus? = null
) {
    suspend operator fun invoke(
        saleId: SaleId,
        tableName: String? = null,
        channelName: String? = null
    ): Result<KitchenTicketResult> = runCatching {
        val sale = saleRepository.getById(saleId) ?: error("Sale not found")
        val unsentLines = sale.unsentLines()
        require(unsentLines.isNotEmpty()) { "Tidak ada item baru untuk dikirim ke dapur" }

        // Mark lines as sent and transition sale status
        val updated = sale.sendToKitchen()
        saleRepository.save(updated)

        // Create kitchen ticket with delta items
        val ticketNumber = kitchenTicketRepository.getNextTicketNumber(sale.outletId)
        val ticketItems = unsentLines.map { line ->
            KitchenTicketItem(
                orderLineId = line.id,
                productName = line.productRef.name,
                quantity = line.quantity,
                modifiers = line.selectedModifiers
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(", ") { it.optionName },
                notes = line.notes
            )
        }
        val ticket = KitchenTicket(
            id = KitchenTicketId.generate(),
            saleId = sale.id,
            outletId = sale.outletId,
            items = ticketItems,
            tableName = tableName,
            channelName = channelName,
            ticketNumber = ticketNumber
        )
        kitchenTicketRepository.save(ticket)

        // Publish domain event
        eventBus?.publish(
            KitchenTicketCreated(
                ticketId = ticket.id,
                saleId = ticket.saleId,
                outletId = ticket.outletId,
                station = ticket.station,
                ticketNumber = ticket.ticketNumber,
                itemCount = ticket.items.size,
                tableName = ticket.tableName,
                channelName = ticket.channelName
            )
        )

        KitchenTicketResult(sale = updated, newLines = unsentLines, ticket = ticket)
    }
}
