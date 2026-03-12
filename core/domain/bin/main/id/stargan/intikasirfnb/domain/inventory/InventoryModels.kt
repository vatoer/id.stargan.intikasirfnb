package id.stargan.intikasirfnb.domain.inventory

import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.catalog.UnitOfMeasure
import id.stargan.intikasirfnb.domain.identity.OutletId
import java.math.BigDecimal

@JvmInline
value class StockLevelId(val value: String)

data class StockLevel(
    val productId: ProductId,
    val outletId: OutletId,
    val quantity: BigDecimal,
    val unitOfMeasure: UnitOfMeasure
)

enum class StockMovementType { IN, OUT, ADJUSTMENT }

data class StockMovement(
    val productId: ProductId,
    val outletId: OutletId,
    val type: StockMovementType,
    val quantity: BigDecimal,
    val referenceType: String? = null,
    val referenceId: String? = null
)
