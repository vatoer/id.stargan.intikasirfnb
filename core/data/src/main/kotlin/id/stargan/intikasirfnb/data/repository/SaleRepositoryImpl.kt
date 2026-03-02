package id.stargan.intikasirfnb.data.repository

import id.stargan.intikasirfnb.data.local.dao.OrderLineDao
import id.stargan.intikasirfnb.data.local.dao.PaymentDao
import id.stargan.intikasirfnb.data.local.dao.SaleDao
import id.stargan.intikasirfnb.data.mapper.toDomain
import id.stargan.intikasirfnb.data.mapper.toEntity
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SaleRepositoryImpl(
    private val saleDao: SaleDao,
    private val orderLineDao: OrderLineDao,
    private val paymentDao: PaymentDao
) : SaleRepository {

    override suspend fun getById(id: SaleId): Sale? {
        val entity = saleDao.getById(id.value) ?: return null
        val lines = orderLineDao.getBySaleId(id.value)
        val payments = paymentDao.getBySaleId(id.value)
        return entity.toDomain(lines, payments)
    }

    override suspend fun save(sale: Sale) {
        saleDao.insert(sale.toEntity())
        orderLineDao.deleteBySaleId(sale.id.value)
        paymentDao.deleteBySaleId(sale.id.value)
        orderLineDao.insertAll(sale.lines.mapIndexed { i, line -> line.toEntity(sale.id.value, i) })
        paymentDao.insertAll(sale.payments.mapIndexed { i, pay -> pay.toEntity(sale.id.value, i) })
    }

    override fun streamByOutlet(outletId: OutletId): Flow<List<Sale>> = saleDao.streamByOutlet(outletId.value).map { list ->
        list.map { entity ->
            val lines = orderLineDao.getBySaleId(entity.id)
            val payments = paymentDao.getBySaleId(entity.id)
            entity.toDomain(lines, payments)
        }
    }

    override suspend fun listByOutlet(outletId: OutletId, limit: Int): List<Sale> = saleDao.listByOutlet(outletId.value, limit).map { entity ->
        val lines = orderLineDao.getBySaleId(entity.id)
        val payments = paymentDao.getBySaleId(entity.id)
        entity.toDomain(lines, payments)
    }
}
