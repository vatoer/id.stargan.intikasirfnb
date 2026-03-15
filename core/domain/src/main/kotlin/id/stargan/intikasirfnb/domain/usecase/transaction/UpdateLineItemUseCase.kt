package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.transaction.DiscountInfo
import id.stargan.intikasirfnb.domain.transaction.OrderLineId
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleRepository
import id.stargan.intikasirfnb.domain.transaction.SelectedAddOn
import id.stargan.intikasirfnb.domain.transaction.SelectedModifier

class UpdateLineItemUseCase(private val saleRepository: SaleRepository) {
    suspend operator fun invoke(
        saleId: SaleId,
        lineId: OrderLineId,
        quantity: Int? = null,
        discountAmount: Money? = null,
        discountInfo: DiscountInfo? = null,
        notes: String? = null,
        selectedModifiers: List<SelectedModifier>? = null,
        selectedAddOns: List<SelectedAddOn>? = null
    ): Result<Sale> = runCatching {
        val sale = saleRepository.getById(saleId) ?: error("Sale not found")
        val updated = sale.updateLine(lineId) { line ->
            // If discountInfo provided, use applyDiscount() to compute amount
            val withDiscount = if (discountInfo != null) {
                line.applyDiscount(discountInfo)
            } else {
                line.copy(discountAmount = discountAmount ?: line.discountAmount)
            }
            withDiscount.copy(
                quantity = quantity ?: line.quantity,
                notes = notes ?: line.notes,
                selectedModifiers = selectedModifiers ?: line.selectedModifiers,
                selectedAddOns = selectedAddOns ?: line.selectedAddOns
            )
        }
        saleRepository.save(updated)
        updated
    }
}
