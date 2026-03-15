package id.stargan.intikasirfnb.data.repository

import id.stargan.intikasirfnb.data.local.dao.StockLevelDao
import id.stargan.intikasirfnb.data.mapper.toDomain
import id.stargan.intikasirfnb.data.mapper.toEntity
import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.inventory.StockLevel
import id.stargan.intikasirfnb.domain.inventory.StockLevelId
import id.stargan.intikasirfnb.domain.inventory.StockLevelRepository

class StockLevelRepositoryImpl(
    private val dao: StockLevelDao
) : StockLevelRepository {
    override suspend fun get(productId: ProductId, outletId: OutletId) =
        dao.getByProductAndOutlet(productId.value, outletId.value)?.toDomain()

    override suspend fun getById(id: StockLevelId) =
        dao.getById(id.value)?.toDomain()

    override suspend fun save(stockLevel: StockLevel) =
        dao.insert(stockLevel.toEntity())

    override suspend fun listByOutlet(outletId: OutletId) =
        dao.listByOutlet(outletId.value).map { it.toDomain() }
}
