package id.stargan.intikasirfnb.domain.catalog

import id.stargan.intikasirfnb.domain.shared.Money

/**
 * Read-only snapshot of a product/menu item for use in Transaction (ACL).
 * Transaction holds this snapshot so it does not depend on Catalog aggregate.
 */
data class ProductRef(
    val productId: ProductId,
    val name: String,
    val price: Money,
    val taxCode: String? = null
) {
    companion object {
        fun from(menuItem: MenuItem): ProductRef = ProductRef(
            productId = menuItem.id,
            name = menuItem.name,
            price = menuItem.basePrice,
            taxCode = menuItem.taxCode
        )
    }
}
