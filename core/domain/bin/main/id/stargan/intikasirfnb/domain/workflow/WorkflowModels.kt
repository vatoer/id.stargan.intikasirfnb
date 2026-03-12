package id.stargan.intikasirfnb.domain.workflow

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.UserId
import id.stargan.intikasirfnb.domain.shared.UlidGenerator
import id.stargan.intikasirfnb.domain.transaction.OrderLineId
import id.stargan.intikasirfnb.domain.transaction.SaleId

@JvmInline
value class KitchenTicketId(val value: String) {
    companion object {
        fun generate() = KitchenTicketId(UlidGenerator.generate())
    }
}

/**
 * Kitchen ticket lifecycle:
 *   PENDING → PREPARING → READY → SERVED
 *
 * - PENDING: ticket created, waiting for kitchen staff to start
 * - PREPARING: kitchen staff is cooking/making the items
 * - READY: items ready for pickup/serving
 * - SERVED: items delivered to customer (terminal state)
 */
enum class KitchenTicketStatus {
    PENDING,
    PREPARING,
    READY,
    SERVED;

    fun canTransitionTo(next: KitchenTicketStatus): Boolean = when (this) {
        PENDING -> next == PREPARING
        PREPARING -> next == READY
        READY -> next == SERVED
        SERVED -> false
    }
}

/**
 * Station type — allows splitting tickets by preparation area.
 * E.g. food goes to kitchen, drinks go to bar.
 */
enum class KitchenStationType {
    KITCHEN,   // makanan
    BAR,       // minuman
    GENERAL    // default, single station
}

/**
 * One line item within a kitchen ticket — snapshot of what to prepare.
 */
data class KitchenTicketItem(
    val id: String = UlidGenerator.generate(),
    val orderLineId: OrderLineId,
    val productName: String,
    val quantity: Int,
    val modifiers: String? = null, // comma-separated modifier names
    val notes: String? = null
)

/**
 * KitchenTicket aggregate root.
 *
 * Created when order is sent to kitchen. One ticket per send action
 * (delta items only — additional items create new tickets).
 * Can optionally be split by station (KITCHEN vs BAR).
 */
data class KitchenTicket(
    val id: KitchenTicketId = KitchenTicketId.generate(),
    val saleId: SaleId,
    val outletId: OutletId,
    val items: List<KitchenTicketItem> = emptyList(),
    val station: KitchenStationType = KitchenStationType.GENERAL,
    val status: KitchenTicketStatus = KitchenTicketStatus.PENDING,
    val assignedTo: UserId? = null,
    val tableName: String? = null,
    val channelName: String? = null,
    val ticketNumber: Int = 0, // sequential per outlet per day
    val createdAtMillis: Long = System.currentTimeMillis(),
    val startedAtMillis: Long? = null,
    val readyAtMillis: Long? = null,
    val servedAtMillis: Long? = null
) {
    /** Total items to prepare */
    val totalQuantity: Int get() = items.sumOf { it.quantity }

    /** Time elapsed since creation (millis) */
    fun elapsedMillis(): Long = System.currentTimeMillis() - createdAtMillis

    /** Preparation time (millis), null if not started */
    fun prepTimeMillis(): Long? {
        val start = startedAtMillis ?: return null
        val end = readyAtMillis ?: System.currentTimeMillis()
        return end - start
    }

    /** Transition to PREPARING */
    fun startPreparing(assignedTo: UserId? = null): KitchenTicket {
        require(status.canTransitionTo(KitchenTicketStatus.PREPARING)) {
            "Cannot start preparing from status $status"
        }
        return copy(
            status = KitchenTicketStatus.PREPARING,
            assignedTo = assignedTo ?: this.assignedTo,
            startedAtMillis = System.currentTimeMillis()
        )
    }

    /** Transition to READY */
    fun markReady(): KitchenTicket {
        require(status.canTransitionTo(KitchenTicketStatus.READY)) {
            "Cannot mark ready from status $status"
        }
        return copy(
            status = KitchenTicketStatus.READY,
            readyAtMillis = System.currentTimeMillis()
        )
    }

    /** Transition to SERVED */
    fun markServed(): KitchenTicket {
        require(status.canTransitionTo(KitchenTicketStatus.SERVED)) {
            "Cannot mark served from status $status"
        }
        return copy(
            status = KitchenTicketStatus.SERVED,
            servedAtMillis = System.currentTimeMillis()
        )
    }
}
