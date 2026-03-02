package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleRepository

class CompleteSaleUseCase(private val saleRepository: SaleRepository) {
    suspend operator fun invoke(saleId: SaleId): Result<Sale> = runCatching {
        val sale = saleRepository.getById(saleId) ?: error("Sale not found")
        val updated = sale.complete()
        saleRepository.save(updated)
        updated
    }
}
