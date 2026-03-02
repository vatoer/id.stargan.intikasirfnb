package id.stargan.intikasirfnb.domain.inventory

import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.identity.OutletId

interface StockLevelRepository {
    suspend fun get(productId: ProductId, outletId: OutletId): StockLevel?
    suspend fun save(stockLevel: StockLevel)
    suspend fun listByOutlet(outletId: OutletId): List<StockLevel>
}

interface StockMovementRepository {
    suspend fun add(movement: StockMovement)
}
