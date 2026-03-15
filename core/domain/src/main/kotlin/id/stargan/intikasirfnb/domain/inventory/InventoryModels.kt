package id.stargan.intikasirfnb.domain.inventory

import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.catalog.UnitOfMeasure
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.shared.UlidGenerator
import java.math.BigDecimal

@JvmInline
value class StockLevelId(val value: String) {
    companion object {
        fun generate() = StockLevelId(UlidGenerator.generate())
    }
}

@JvmInline
value class StockMovementId(val value: String) {
    companion object {
        fun generate() = StockMovementId(UlidGenerator.generate())
    }
}

/**
 * Current stock level for a product at an outlet.
 * Updated by stock movements (receive, adjustment, sale deduction).
 */
data class StockLevel(
    val id: StockLevelId = StockLevelId.generate(),
    val productId: ProductId,
    val outletId: OutletId,
    val productName: String = "",
    val quantity: BigDecimal,
    val unitOfMeasure: UnitOfMeasure = UnitOfMeasure.PCS,
    val lowStockThreshold: BigDecimal = BigDecimal.ZERO
) {
    val isLowStock: Boolean get() = lowStockThreshold > BigDecimal.ZERO && quantity <= lowStockThreshold
}

enum class StockMovementType {
    RECEIVE,     // barang masuk (dari supplier / pembelian)
    SALE,        // barang keluar (otomatis dari penjualan)
    ADJUSTMENT,  // koreksi stok (stock opname)
    WASTE,       // barang rusak / kadaluarsa
    TRANSFER     // transfer antar outlet (future)
}

/**
 * Individual stock movement — audit trail for every stock change.
 */
data class StockMovement(
    val id: StockMovementId = StockMovementId.generate(),
    val productId: ProductId,
    val outletId: OutletId,
    val type: StockMovementType,
    val quantity: BigDecimal,
    val notes: String? = null,
    val referenceType: String? = null,  // "SALE", "PURCHASE_ORDER", etc.
    val referenceId: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)
