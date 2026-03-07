package id.stargan.intikasirfnb.data.repository

import id.stargan.intikasirfnb.data.local.dao.CashierSessionDao
import id.stargan.intikasirfnb.data.mapper.toDomain
import id.stargan.intikasirfnb.data.mapper.toEntity
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.TerminalId
import id.stargan.intikasirfnb.domain.transaction.CashierSession
import id.stargan.intikasirfnb.domain.transaction.CashierSessionId
import id.stargan.intikasirfnb.domain.transaction.CashierSessionRepository

class CashierSessionRepositoryImpl(
    private val dao: CashierSessionDao
) : CashierSessionRepository {

    override suspend fun getById(id: CashierSessionId): CashierSession? =
        dao.getById(id.value)?.toDomain()

    override suspend fun getCurrentSession(outletId: OutletId, terminalId: TerminalId): CashierSession? =
        dao.getCurrentSession(outletId.value, terminalId.value)?.toDomain()

    override suspend fun save(session: CashierSession) {
        dao.insert(session.toEntity())
    }

    override suspend fun listByOutlet(outletId: OutletId, limit: Int): List<CashierSession> =
        dao.listByOutlet(outletId.value, limit).map { it.toDomain() }
}
