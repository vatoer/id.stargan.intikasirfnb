package id.stargan.intikasirfnb.domain.transaction

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.identity.TerminalId
import kotlinx.coroutines.flow.Flow

interface SalesChannelRepository {
    suspend fun getById(id: SalesChannelId): SalesChannel?
    suspend fun save(channel: SalesChannel)
    suspend fun listByTenant(tenantId: TenantId): List<SalesChannel>
    suspend fun delete(id: SalesChannelId)
}

interface SaleRepository {
    suspend fun getById(id: SaleId): Sale?
    suspend fun save(sale: Sale)
    fun streamByOutlet(outletId: OutletId): Flow<List<Sale>>
    suspend fun listByOutlet(outletId: OutletId, limit: Int = 100): List<Sale>
    suspend fun listOpenByOutlet(outletId: OutletId): List<Sale>
    fun streamOpenByOutlet(outletId: OutletId): Flow<List<Sale>>
}

interface CashierSessionRepository {
    suspend fun getById(id: CashierSessionId): CashierSession?
    suspend fun getCurrentSession(outletId: OutletId, terminalId: TerminalId): CashierSession?
    suspend fun save(session: CashierSession)
    suspend fun listByOutlet(outletId: OutletId, limit: Int = 50): List<CashierSession>
}

interface TableRepository {
    suspend fun getById(id: TableId): Table?
    suspend fun save(table: Table)
    suspend fun listByOutlet(outletId: OutletId): List<Table>
    suspend fun getTableWithActiveSale(tableId: TableId): Table?
}
