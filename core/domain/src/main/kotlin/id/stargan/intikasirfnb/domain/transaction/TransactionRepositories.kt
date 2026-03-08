package id.stargan.intikasirfnb.domain.transaction

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.identity.TerminalId
import id.stargan.intikasirfnb.domain.shared.Money
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
    suspend fun delete(id: TableId)
    suspend fun listByOutlet(outletId: OutletId): List<Table>
    fun streamByOutlet(outletId: OutletId): Flow<List<Table>>
    suspend fun listAvailable(outletId: OutletId): List<Table>
    suspend fun listOccupied(outletId: OutletId): List<Table>
    suspend fun findBySaleId(saleId: SaleId): Table?
    suspend fun occupyTable(tableId: TableId, saleId: SaleId)
    suspend fun releaseTable(tableId: TableId)
    suspend fun releaseBySaleId(saleId: SaleId)
    suspend fun listSections(outletId: OutletId): List<String>
}

interface PlatformSettlementRepository {
    suspend fun getById(id: PlatformSettlementId): PlatformSettlement?
    suspend fun save(settlement: PlatformSettlement)
    suspend fun listByOutlet(outletId: OutletId, limit: Int = 50): List<PlatformSettlement>
    suspend fun listByChannel(channelId: SalesChannelId, limit: Int = 50): List<PlatformSettlement>
    suspend fun listByStatus(outletId: OutletId, status: SettlementStatus): List<PlatformSettlement>
    suspend fun listPending(outletId: OutletId): List<PlatformSettlement>
    suspend fun listPendingByChannel(channelId: SalesChannelId): List<PlatformSettlement>
    suspend fun listByDateRange(outletId: OutletId, fromMillis: Long, toMillis: Long): List<PlatformSettlement>
    suspend fun totalPendingAmount(outletId: OutletId): Money
    suspend fun totalPendingAmountByChannel(channelId: SalesChannelId): Money
    suspend fun totalSettledAmountInRange(outletId: OutletId, fromMillis: Long, toMillis: Long): Money
    suspend fun totalCommissionInRange(outletId: OutletId, fromMillis: Long, toMillis: Long): Money
    suspend fun countPending(outletId: OutletId): Int
}
