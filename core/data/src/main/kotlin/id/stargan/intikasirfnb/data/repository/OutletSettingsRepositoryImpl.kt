package id.stargan.intikasirfnb.data.repository

import id.stargan.intikasirfnb.data.local.dao.OutletSettingsDao
import id.stargan.intikasirfnb.data.mapper.toDomain
import id.stargan.intikasirfnb.data.mapper.toEntity
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.settings.OutletSettings
import id.stargan.intikasirfnb.domain.settings.OutletSettingsRepository

class OutletSettingsRepositoryImpl(private val dao: OutletSettingsDao) : OutletSettingsRepository {
    override suspend fun getByOutletId(outletId: OutletId): OutletSettings? = dao.getByOutletId(outletId.value)?.toDomain()
    override suspend fun save(settings: OutletSettings) { dao.insert(settings.toEntity()) }
}
