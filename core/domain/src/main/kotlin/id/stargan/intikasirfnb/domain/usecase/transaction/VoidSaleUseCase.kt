package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleRepository
import id.stargan.intikasirfnb.domain.transaction.TableRepository

class VoidSaleUseCase(
    private val saleRepository: SaleRepository,
    private val tableRepository: TableRepository? = null
) {
    suspend operator fun invoke(saleId: SaleId): Result<Sale> = runCatching {
        val sale = saleRepository.getById(saleId) ?: error("Sale not found")
        val updated = sale.void()
        saleRepository.save(updated)

        // Release table if dine-in
        if (updated.tableId != null && tableRepository != null) {
            tableRepository.releaseBySaleId(updated.id)
        }

        updated
    }
}
