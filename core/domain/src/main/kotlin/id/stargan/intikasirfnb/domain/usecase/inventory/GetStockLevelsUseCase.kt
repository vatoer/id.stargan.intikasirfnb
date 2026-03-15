package id.stargan.intikasirfnb.domain.usecase.inventory

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.inventory.StockLevel
import id.stargan.intikasirfnb.domain.inventory.StockLevelRepository

class GetStockLevelsUseCase(
    private val stockLevelRepository: StockLevelRepository
) {
    suspend operator fun invoke(outletId: OutletId): List<StockLevel> =
        stockLevelRepository.listByOutlet(outletId)
}
