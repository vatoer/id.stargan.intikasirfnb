package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleRepository

/**
 * Konfirmasi sale (draft → confirmed). Bisa trigger pembuatan KitchenTicket di layer aplikasi.
 */
class ConfirmSaleUseCase(
    private val saleRepository: SaleRepository
) {
    suspend operator fun invoke(saleId: SaleId): Result<Sale> = runCatching {
        val sale = saleRepository.getById(saleId) ?: error("Sale not found")
        val updated = sale.confirm()
        saleRepository.save(updated)
        updated
    }
}
