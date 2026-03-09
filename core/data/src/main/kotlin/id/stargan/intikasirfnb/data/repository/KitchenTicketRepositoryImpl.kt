package id.stargan.intikasirfnb.data.repository

import id.stargan.intikasirfnb.data.local.dao.KitchenTicketDao
import id.stargan.intikasirfnb.data.mapper.toDomain
import id.stargan.intikasirfnb.data.mapper.toEntity
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.workflow.KitchenTicket
import id.stargan.intikasirfnb.domain.workflow.KitchenTicketId
import id.stargan.intikasirfnb.domain.workflow.KitchenTicketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class KitchenTicketRepositoryImpl(
    private val dao: KitchenTicketDao
) : KitchenTicketRepository {

    override suspend fun getById(id: KitchenTicketId): KitchenTicket? {
        val entity = dao.getById(id.value) ?: return null
        val items = dao.getItemsByTicketId(id.value).map { it.toDomain() }
        return entity.toDomain(items)
    }

    override suspend fun save(ticket: KitchenTicket) {
        dao.insertTicket(ticket.toEntity())
        dao.deleteItemsByTicketId(ticket.id.value)
        if (ticket.items.isNotEmpty()) {
            dao.insertItems(ticket.items.map { it.toEntity(ticket.id.value) })
        }
    }

    override suspend fun getBySaleId(saleId: SaleId): List<KitchenTicket> {
        return dao.getBySaleId(saleId.value).map { entity ->
            val items = dao.getItemsByTicketId(entity.id).map { it.toDomain() }
            entity.toDomain(items)
        }
    }

    override suspend fun getActiveByOutlet(outletId: OutletId): List<KitchenTicket> {
        return dao.getActiveByOutlet(outletId.value).map { entity ->
            val items = dao.getItemsByTicketId(entity.id).map { it.toDomain() }
            entity.toDomain(items)
        }
    }

    override suspend fun getNextTicketNumber(outletId: OutletId): Int {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return dao.getNextTicketNumber(outletId.value, todayStart)
    }

    override fun streamActiveByOutlet(outletId: OutletId): Flow<List<KitchenTicket>> {
        return dao.streamActiveByOutlet(outletId.value).map { entities ->
            entities.map { entity ->
                val items = dao.getItemsByTicketId(entity.id).map { it.toDomain() }
                entity.toDomain(items)
            }
        }
    }
}
