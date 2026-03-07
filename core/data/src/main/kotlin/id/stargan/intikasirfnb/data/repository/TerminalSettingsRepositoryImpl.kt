package id.stargan.intikasirfnb.data.repository

import id.stargan.intikasirfnb.data.local.dao.TerminalSettingsDao
import id.stargan.intikasirfnb.data.mapper.toDomain
import id.stargan.intikasirfnb.data.mapper.toEntity
import id.stargan.intikasirfnb.domain.identity.TerminalId
import id.stargan.intikasirfnb.domain.settings.TerminalSettings
import id.stargan.intikasirfnb.domain.settings.TerminalSettingsRepository

class TerminalSettingsRepositoryImpl(private val dao: TerminalSettingsDao) : TerminalSettingsRepository {
    override suspend fun getByTerminalId(terminalId: TerminalId): TerminalSettings? = dao.getByTerminalId(terminalId.value)?.toDomain()
    override suspend fun save(settings: TerminalSettings) { dao.insert(settings.toEntity()) }
}
