package id.stargan.intikasirfnb.domain.workflow

import id.stargan.intikasirfnb.domain.identity.OutletId
import kotlinx.coroutines.flow.Flow

interface KitchenTicketRepository {
    suspend fun getById(id: KitchenTicketId): KitchenTicket?
    suspend fun save(ticket: KitchenTicket)
    fun streamPendingByOutlet(outletId: OutletId): Flow<List<KitchenTicket>>
}
