package id.stargan.intikasirfnb.data.repository

import id.stargan.intikasirfnb.data.local.dao.TerminalDao
import id.stargan.intikasirfnb.data.mapper.toDomain
import id.stargan.intikasirfnb.data.mapper.toEntity
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.Terminal
import id.stargan.intikasirfnb.domain.identity.TerminalId
import id.stargan.intikasirfnb.domain.identity.TerminalRepository

class TerminalRepositoryImpl(private val dao: TerminalDao) : TerminalRepository {
    override suspend fun getById(id: TerminalId): Terminal? = dao.getById(id.value)?.toDomain()
    override suspend fun save(terminal: Terminal) { dao.insert(terminal.toEntity()) }
    override suspend fun getByOutlet(outletId: OutletId): List<Terminal> =
        dao.getByOutlet(outletId.value).map { it.toDomain() }
    override suspend fun getActiveByOutlet(outletId: OutletId): List<Terminal> =
        dao.getActiveByOutlet(outletId.value).map { it.toDomain() }
}
