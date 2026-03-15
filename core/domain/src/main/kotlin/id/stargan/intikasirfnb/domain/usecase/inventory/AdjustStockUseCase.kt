package id.stargan.intikasirfnb.domain.usecase.inventory

import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.inventory.StockLevel
import id.stargan.intikasirfnb.domain.inventory.StockLevelRepository
import id.stargan.intikasirfnb.domain.inventory.StockMovement
import id.stargan.intikasirfnb.domain.inventory.StockMovementRepository
import id.stargan.intikasirfnb.domain.inventory.StockMovementType
import java.math.BigDecimal

/**
 * Adjust stock level — for stock opname (count), waste, or manual correction.
 * Records a movement and updates the stock level.
 */
class AdjustStockUseCase(
    private val stockLevelRepository: StockLevelRepository,
    private val stockMovementRepository: StockMovementRepository
) {
    suspend operator fun invoke(
        productId: ProductId,
        outletId: OutletId,
        newQuantity: BigDecimal,
        type: StockMovementType = StockMovementType.ADJUSTMENT,
        notes: String? = null
    ): Result<StockLevel> = runCatching {
        val current = stockLevelRepository.get(productId, outletId)
        val oldQty = current?.quantity ?: BigDecimal.ZERO
        val delta = newQuantity - oldQty

        // Record movement
        stockMovementRepository.add(
            StockMovement(
                productId = productId,
                outletId = outletId,
                type = type,
                quantity = delta,
                notes = notes
            )
        )

        // Update level
        val updated = current?.copy(quantity = newQuantity)
            ?: StockLevel(productId = productId, outletId = outletId, quantity = newQuantity)
        stockLevelRepository.save(updated)
        updated
    }
}
