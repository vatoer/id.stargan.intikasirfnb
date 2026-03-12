package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.transaction.TableId
import id.stargan.intikasirfnb.domain.transaction.TableRepository

/**
 * Soft-deletes a table. Cannot delete occupied tables.
 */
class DeleteTableUseCase(
    private val tableRepository: TableRepository
) {
    suspend operator fun invoke(tableId: TableId): Result<Unit> = runCatching {
        val table = tableRepository.getById(tableId) ?: error("Meja tidak ditemukan")
        require(table.isAvailable) { "Tidak bisa hapus meja yang sedang digunakan" }
        tableRepository.delete(tableId)
    }
}
