package id.stargan.intikasirfnb.domain.catalog

import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.shared.UlidGenerator
import java.math.BigDecimal

// --- IDs ---

@JvmInline
value class PriceListId(val value: String) {
    companion object {
        fun generate() = PriceListId(UlidGenerator.generate())
    }
}

// --- PriceListEntry — one item's price in a price list ---

data class PriceListEntry(
    val productId: ProductId,
    val price: Money
) {
    init {
        require(price.amount >= BigDecimal.ZERO) { "Price must not be negative" }
    }
}

// --- PriceList aggregate ---

/**
 * A named collection of per-item price overrides.
 * Assigned to a SalesChannel via SalesChannel.priceListId.
 *
 * Resolution order (in SalesChannel):
 *   1. PriceList entry for this productId → full override
 *   2. Fallback → base price + channel adjustment (markup/discount)
 *
 * Use cases:
 *   - GoFood channel: set all items 20% higher than dine-in
 *     → use priceAdjustmentType=MARKUP_PERCENT on channel (no PriceList needed)
 *   - GoFood channel: some items have custom prices, others use markup
 *     → create PriceList with specific items, channel still has markup as fallback
 *   - Special event pricing: create a PriceList, assign to channel temporarily
 */
data class PriceList(
    val id: PriceListId,
    val tenantId: TenantId,
    val name: String,
    val description: String? = null,
    val entries: List<PriceListEntry> = emptyList(),
    val isActive: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    init {
        require(name.isNotBlank()) { "PriceList name must not be blank" }
        // Ensure no duplicate products
        val productIds = entries.map { it.productId }
        require(productIds.size == productIds.toSet().size) {
            "Duplicate product entries in PriceList"
        }
    }

    /** Lookup price for a specific product. Returns null if not in this list. */
    fun priceFor(productId: ProductId): Money? =
        entries.find { it.productId == productId }?.price

    /** Add or update a price entry */
    fun setPrice(productId: ProductId, price: Money): PriceList {
        val updated = entries.filter { it.productId != productId } +
            PriceListEntry(productId, price)
        return copy(entries = updated, updatedAtMillis = System.currentTimeMillis())
    }

    /** Remove a price entry (item will fall back to channel adjustment) */
    fun removePrice(productId: ProductId): PriceList {
        return copy(
            entries = entries.filter { it.productId != productId },
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    /** Number of items with custom prices */
    val entryCount: Int get() = entries.size
}
