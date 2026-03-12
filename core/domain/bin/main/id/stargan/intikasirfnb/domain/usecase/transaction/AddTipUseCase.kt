package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleRepository
import id.stargan.intikasirfnb.domain.transaction.TipLine

class AddTipUseCase(private val saleRepository: SaleRepository) {
    suspend operator fun invoke(saleId: SaleId, tipAmount: Money): Result<Sale> = runCatching {
        val sale = saleRepository.getById(saleId) ?: error("Sale not found")
        val updated = sale.addTip(TipLine(amount = tipAmount))
        saleRepository.save(updated)
        updated
    }
}
