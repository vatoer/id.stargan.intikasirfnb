package id.stargan.intikasirfnb.domain.transaction

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.TerminalId
import kotlinx.coroutines.flow.Flow

interface SaleRepository {
    suspend fun getById(id: SaleId): Sale?
    suspend fun save(sale: Sale)
    fun streamByOutlet(outletId: OutletId): Flow<List<Sale>>
    suspend fun listByOutlet(outletId: OutletId, limit: Int = 100): List<Sale>
}

interface CashierSessionRepository {
    suspend fun getCurrentSession(outletId: OutletId, terminalId: TerminalId): CashierSession?
    suspend fun save(session: CashierSession)
}

interface TableRepository {
    suspend fun getById(id: TableId): Table?
    suspend fun save(table: Table)
    suspend fun listByOutlet(outletId: OutletId): List<Table>
    suspend fun getTableWithActiveSale(tableId: TableId): Table?
}
