package id.stargan.intikasirfnb.data.repository

import id.stargan.intikasirfnb.data.local.dao.PlatformSettlementDao
import id.stargan.intikasirfnb.data.mapper.toDomain
import id.stargan.intikasirfnb.data.mapper.toEntity
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.shared.Money
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlement
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlementId
import id.stargan.intikasirfnb.domain.transaction.PlatformSettlementRepository
import id.stargan.intikasirfnb.domain.transaction.SalesChannelId
import id.stargan.intikasirfnb.domain.transaction.SettlementStatus
import java.math.BigDecimal

class PlatformSettlementRepositoryImpl(
    private val dao: PlatformSettlementDao
) : PlatformSettlementRepository {

    override suspend fun getById(id: PlatformSettlementId): PlatformSettlement? =
        dao.getById(id.value)?.toDomain()

    override suspend fun save(settlement: PlatformSettlement) {
        dao.insert(settlement.toEntity())
    }

    override suspend fun listByOutlet(outletId: OutletId, limit: Int): List<PlatformSettlement> =
        dao.listByOutlet(outletId.value, limit).map { it.toDomain() }

    override suspend fun listByChannel(channelId: SalesChannelId, limit: Int): List<PlatformSettlement> =
        dao.listByChannel(channelId.value, limit).map { it.toDomain() }

    override suspend fun listByStatus(outletId: OutletId, status: SettlementStatus): List<PlatformSettlement> =
        dao.listByStatus(outletId.value, status.name).map { it.toDomain() }

    override suspend fun listPending(outletId: OutletId): List<PlatformSettlement> =
        dao.listPending(outletId.value).map { it.toDomain() }

    override suspend fun listPendingByChannel(channelId: SalesChannelId): List<PlatformSettlement> =
        dao.listPendingByChannel(channelId.value).map { it.toDomain() }

    override suspend fun listByDateRange(outletId: OutletId, fromMillis: Long, toMillis: Long): List<PlatformSettlement> =
        dao.listByDateRange(outletId.value, fromMillis, toMillis).map { it.toDomain() }

    override suspend fun totalPendingAmount(outletId: OutletId): Money =
        Money(BigDecimal.valueOf(dao.totalPendingAmount(outletId.value)))

    override suspend fun totalPendingAmountByChannel(channelId: SalesChannelId): Money =
        Money(BigDecimal.valueOf(dao.totalPendingAmountByChannel(channelId.value)))

    override suspend fun totalSettledAmountInRange(outletId: OutletId, fromMillis: Long, toMillis: Long): Money =
        Money(BigDecimal.valueOf(dao.totalSettledAmountInRange(outletId.value, fromMillis, toMillis)))

    override suspend fun totalCommissionInRange(outletId: OutletId, fromMillis: Long, toMillis: Long): Money =
        Money(BigDecimal.valueOf(dao.totalCommissionInRange(outletId.value, fromMillis, toMillis)))

    override suspend fun countPending(outletId: OutletId): Int =
        dao.countPending(outletId.value)
}
