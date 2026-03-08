package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.transaction.Sale
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.SaleRepository
import id.stargan.intikasirfnb.domain.transaction.Table
import id.stargan.intikasirfnb.domain.transaction.TableId
import id.stargan.intikasirfnb.domain.transaction.TableRepository

/**
 * Assigns a table to a sale and marks the table as occupied.
 * Releases previous table if the sale was on a different table.
 */
class AssignTableUseCase(
    private val saleRepository: SaleRepository,
    private val tableRepository: TableRepository
) {
    suspend operator fun invoke(saleId: SaleId, tableId: TableId): Result<Pair<Sale, Table>> = runCatching {
        val sale = saleRepository.getById(saleId) ?: error("Sale not found")
        val table = tableRepository.getById(tableId) ?: error("Meja tidak ditemukan")
        require(table.isActive) { "Meja ${table.name} tidak aktif" }

        // Release old table if switching tables
        val oldTableId = sale.tableId
        if (oldTableId != null && oldTableId != tableId) {
            tableRepository.releaseTable(oldTableId)
        }

        // Occupy new table (validates availability)
        val occupiedTable = table.occupy(sale.id)
        tableRepository.save(occupiedTable)

        // Update sale with table reference
        val updatedSale = sale.assignTable(tableId)
        saleRepository.save(updatedSale)

        updatedSale to occupiedTable
    }
}
