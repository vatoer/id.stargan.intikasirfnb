package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.catalog.MenuItem
import id.stargan.intikasirfnb.domain.catalog.ProductRef
import id.stargan.intikasirfnb.domain.transaction.OrderLine
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleRepository

class AddLineItemUseCase(private val saleRepository: SaleRepository) {
    suspend operator fun invoke(
        saleId: SaleId,
        menuItem: MenuItem,
        quantity: Int,
        modifierSnapshot: String? = null
    ): Result<Sale> = runCatching {
        require(quantity > 0) { "Quantity must be positive" }
        val sale = saleRepository.getById(saleId) ?: error("Sale not found")
        val productRef = ProductRef.from(menuItem)
        val line = OrderLine(
            productRef = productRef,
            quantity = quantity,
            unitPrice = productRef.price,
            modifierSnapshot = modifierSnapshot
        )
        val updated = sale.addLine(line)
        saleRepository.save(updated)
        updated
    }
}
