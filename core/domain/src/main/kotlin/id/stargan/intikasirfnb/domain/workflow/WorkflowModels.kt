package id.stargan.intikasirfnb.domain.workflow

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.UserId
import id.stargan.intikasirfnb.domain.transaction.SaleId
import java.util.UUID

@JvmInline
value class KitchenTicketId(val value: String) {
    companion object {
        fun generate() = KitchenTicketId(UUID.randomUUID().toString())
    }
}

enum class KitchenTicketStatus { PENDING, IN_PROGRESS, COMPLETED }

/**
 * F&B: one ticket per order (or per station). Created when order is confirmed.
 */
data class KitchenTicket(
    val id: KitchenTicketId,
    val saleId: SaleId,
    val outletId: OutletId,
    val status: KitchenTicketStatus = KitchenTicketStatus.PENDING,
    val assignedTo: UserId? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val completedAtMillis: Long? = null
)
