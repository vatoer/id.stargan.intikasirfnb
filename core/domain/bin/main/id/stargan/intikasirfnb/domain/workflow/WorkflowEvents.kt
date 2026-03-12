package id.stargan.intikasirfnb.domain.workflow

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.UserId
import id.stargan.intikasirfnb.domain.shared.DomainEvent
import id.stargan.intikasirfnb.domain.transaction.SaleId

/**
 * Published when a kitchen ticket is created (order sent to kitchen).
 */
data class KitchenTicketCreated(
    val ticketId: KitchenTicketId,
    val saleId: SaleId,
    val outletId: OutletId,
    val station: KitchenStationType,
    val ticketNumber: Int,
    val itemCount: Int,
    val tableName: String?,
    val channelName: String?
) : DomainEvent

/**
 * Published when kitchen staff starts preparing a ticket.
 * (PENDING → PREPARING)
 */
data class KitchenTicketStarted(
    val ticketId: KitchenTicketId,
    val saleId: SaleId,
    val outletId: OutletId,
    val assignedTo: UserId?,
    val waitTimeMillis: Long // time spent in PENDING
) : DomainEvent

/**
 * Published when kitchen ticket items are ready for pickup/serving.
 * (PREPARING → READY)
 */
data class KitchenTicketReady(
    val ticketId: KitchenTicketId,
    val saleId: SaleId,
    val outletId: OutletId,
    val prepTimeMillis: Long // time spent PREPARING
) : DomainEvent

/**
 * Published when items have been served to the customer.
 * (READY → SERVED)
 */
data class KitchenTicketServed(
    val ticketId: KitchenTicketId,
    val saleId: SaleId,
    val outletId: OutletId,
    val totalTimeMillis: Long // total time from creation to served
) : DomainEvent
