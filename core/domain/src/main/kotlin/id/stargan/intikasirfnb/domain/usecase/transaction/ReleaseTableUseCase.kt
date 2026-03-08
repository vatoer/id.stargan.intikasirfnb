package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleRepository
import id.stargan.intikasirfnb.domain.transaction.TableRepository

/**
 * Releases the table associated with a sale.
 * Called when a sale is completed or voided, or when manually clearing a table.
 */
class ReleaseTableUseCase(
    private val saleRepository: SaleRepository,
    private val tableRepository: TableRepository
) {
    suspend operator fun invoke(saleId: SaleId): Result<Unit> = runCatching {
        val sale = saleRepository.getById(saleId) ?: error("Sale not found")
        val tableId = sale.tableId ?: return@runCatching

        tableRepository.releaseTable(tableId)

        // Clear tableId on sale
        val updated = sale.assignTable(null)
        saleRepository.save(updated)
    }
}
