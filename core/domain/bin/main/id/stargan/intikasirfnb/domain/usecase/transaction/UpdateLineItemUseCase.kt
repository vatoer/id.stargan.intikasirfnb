package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.transaction.OrderLineId
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleRepository
import id.stargan.intikasirfnb.domain.transaction.SelectedModifier

class UpdateLineItemUseCase(private val saleRepository: SaleRepository) {
    suspend operator fun invoke(
        saleId: SaleId,
        lineId: OrderLineId,
        quantity: Int? = null,
        discountAmount: Money? = null,
        notes: String? = null,
        selectedModifiers: List<SelectedModifier>? = null
    ): Result<Sale> = runCatching {
        val sale = saleRepository.getById(saleId) ?: error("Sale not found")
        val updated = sale.updateLine(lineId) { line ->
            line.copy(
                quantity = quantity ?: line.quantity,
                discountAmount = discountAmount ?: line.discountAmount,
                notes = notes ?: line.notes,
                selectedModifiers = selectedModifiers ?: line.selectedModifiers
            )
        }
        saleRepository.save(updated)
        updated
    }
}
