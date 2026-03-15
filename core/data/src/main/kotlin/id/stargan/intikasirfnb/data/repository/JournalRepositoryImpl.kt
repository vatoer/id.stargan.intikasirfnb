package id.stargan.intikasirfnb.data.repository

import id.stargan.intikasirfnb.data.local.dao.JournalDao
import id.stargan.intikasirfnb.data.mapper.toDomain
import id.stargan.intikasirfnb.data.mapper.toEntity
import id.stargan.intikasirfnb.domain.accounting.Journal
import id.stargan.intikasirfnb.domain.accounting.JournalId
import id.stargan.intikasirfnb.domain.accounting.JournalRepository
import id.stargan.intikasirfnb.domain.identity.OutletId

class JournalRepositoryImpl(private val dao: JournalDao) : JournalRepository {
    override suspend fun save(journal: Journal) = dao.insert(journal.toEntity())
    override suspend fun getById(id: JournalId) = dao.getById(id.value)?.toDomain()
    override suspend fun listByOutlet(outletId: OutletId, limit: Int) =
        dao.listByOutlet(outletId.value, limit).map { it.toDomain() }
    override suspend fun listByDateRange(outletId: OutletId, fromMillis: Long, toMillis: Long) =
        dao.listByDateRange(outletId.value, fromMillis, toMillis).map { it.toDomain() }
}
