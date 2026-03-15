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
 * Receive stock — add incoming quantity (from supplier/purchase).
 */
class ReceiveStockUseCase(
    private val stockLevelRepository: StockLevelRepository,
    private val stockMovementRepository: StockMovementRepository
) {
    suspend operator fun invoke(
        productId: ProductId,
        outletId: OutletId,
        quantity: BigDecimal,
        notes: String? = null,
        referenceId: String? = null
    ): Result<StockLevel> = runCatching {
        require(quantity > BigDecimal.ZERO) { "Quantity must be positive" }

        stockMovementRepository.add(
            StockMovement(
                productId = productId,
                outletId = outletId,
                type = StockMovementType.RECEIVE,
                quantity = quantity,
                notes = notes,
                referenceType = if (referenceId != null) "PURCHASE_ORDER" else null,
                referenceId = referenceId
            )
        )

        val current = stockLevelRepository.get(productId, outletId)
        val newQty = (current?.quantity ?: BigDecimal.ZERO) + quantity
        val updated = current?.copy(quantity = newQty)
            ?: StockLevel(productId = productId, outletId = outletId, quantity = newQty)
        stockLevelRepository.save(updated)
        updated
    }
}
