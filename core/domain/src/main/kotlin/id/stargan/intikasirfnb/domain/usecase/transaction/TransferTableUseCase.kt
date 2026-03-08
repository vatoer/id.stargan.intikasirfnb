package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleRepository
import id.stargan.intikasirfnb.domain.transaction.Table
import id.stargan.intikasirfnb.domain.transaction.TableId
import id.stargan.intikasirfnb.domain.transaction.TableRepository

/**
 * Transfers a sale from one table to another (e.g., guest moves tables).
 * Releases old table and occupies new table atomically.
 */
class TransferTableUseCase(
    private val saleRepository: SaleRepository,
    private val tableRepository: TableRepository
) {
    suspend operator fun invoke(
        saleId: SaleId,
        newTableId: TableId
    ): Result<Pair<Sale, Table>> = runCatching {
        val sale = saleRepository.getById(saleId) ?: error("Sale not found")
        val oldTableId = sale.tableId
        require(oldTableId != newTableId) { "Meja tujuan sama dengan meja saat ini" }

        val newTable = tableRepository.getById(newTableId) ?: error("Meja tujuan tidak ditemukan")
        require(newTable.isAvailable) { "Meja ${newTable.name} sedang digunakan" }

        // Release old table
        if (oldTableId != null) {
            tableRepository.releaseTable(oldTableId)
        }

        // Occupy new table
        val occupiedTable = newTable.occupy(sale.id)
        tableRepository.save(occupiedTable)

        // Update sale
        val updatedSale = sale.assignTable(newTableId)
        saleRepository.save(updatedSale)

        updatedSale to occupiedTable
    }
}
