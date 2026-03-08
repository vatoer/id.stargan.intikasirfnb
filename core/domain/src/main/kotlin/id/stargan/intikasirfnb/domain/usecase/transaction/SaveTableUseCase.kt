package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.transaction.Table
import id.stargan.intikasirfnb.domain.transaction.TableRepository

/**
 * Saves (creates or updates) a table in the outlet.
 */
class SaveTableUseCase(
    private val tableRepository: TableRepository
) {
    suspend operator fun invoke(table: Table): Result<Table> = runCatching {
        require(table.name.isNotBlank()) { "Nama meja tidak boleh kosong" }
        tableRepository.save(table)
        table
    }
}
