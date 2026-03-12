package id.stargan.intikasirfnb.domain.workflow

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.transaction.SaleId
import kotlinx.coroutines.flow.Flow

interface KitchenTicketRepository {
    suspend fun getById(id: KitchenTicketId): KitchenTicket?
    suspend fun save(ticket: KitchenTicket)
    suspend fun getBySaleId(saleId: SaleId): List<KitchenTicket>
    suspend fun getActiveByOutlet(outletId: OutletId): List<KitchenTicket>
    suspend fun getNextTicketNumber(outletId: OutletId): Int
    fun streamActiveByOutlet(outletId: OutletId): Flow<List<KitchenTicket>>
}
