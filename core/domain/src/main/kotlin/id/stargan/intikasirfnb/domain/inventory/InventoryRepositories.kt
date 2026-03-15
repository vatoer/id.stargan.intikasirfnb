package id.stargan.intikasirfnb.domain.inventory

import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.identity.OutletId

interface StockLevelRepository {
    suspend fun get(productId: ProductId, outletId: OutletId): StockLevel?
    suspend fun getById(id: StockLevelId): StockLevel?
    suspend fun save(stockLevel: StockLevel)
    suspend fun listByOutlet(outletId: OutletId): List<StockLevel>
}

interface StockMovementRepository {
    suspend fun add(movement: StockMovement)
    suspend fun listByProduct(productId: ProductId, outletId: OutletId, limit: Int = 50): List<StockMovement>
    suspend fun listByOutlet(outletId: OutletId, limit: Int = 100): List<StockMovement>
}
