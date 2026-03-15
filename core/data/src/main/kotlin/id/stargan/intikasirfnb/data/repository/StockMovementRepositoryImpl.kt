package id.stargan.intikasirfnb.data.repository

import id.stargan.intikasirfnb.data.local.dao.StockMovementDao
import id.stargan.intikasirfnb.data.mapper.toDomain
import id.stargan.intikasirfnb.data.mapper.toEntity
import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.inventory.StockMovement
import id.stargan.intikasirfnb.domain.inventory.StockMovementRepository

class StockMovementRepositoryImpl(
    private val dao: StockMovementDao
) : StockMovementRepository {
    override suspend fun add(movement: StockMovement) =
        dao.insert(movement.toEntity())

    override suspend fun listByProduct(productId: ProductId, outletId: OutletId, limit: Int) =
        dao.listByProduct(productId.value, outletId.value, limit).map { it.toDomain() }

    override suspend fun listByOutlet(outletId: OutletId, limit: Int) =
        dao.listByOutlet(outletId.value, limit).map { it.toDomain() }
}
