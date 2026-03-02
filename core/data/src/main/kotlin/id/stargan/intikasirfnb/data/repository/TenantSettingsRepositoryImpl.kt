package id.stargan.intikasirfnb.data.repository

import id.stargan.intikasirfnb.data.local.dao.TenantSettingsDao
import id.stargan.intikasirfnb.data.mapper.toDomain
import id.stargan.intikasirfnb.data.mapper.toEntity
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.settings.TenantSettings
import id.stargan.intikasirfnb.domain.settings.TenantSettingsRepository

class TenantSettingsRepositoryImpl(private val dao: TenantSettingsDao) : TenantSettingsRepository {
    override suspend fun getByTenantId(tenantId: TenantId): TenantSettings? = dao.getByTenantId(tenantId.value)?.toDomain()
    override suspend fun save(settings: TenantSettings) { dao.insert(settings.toEntity()) }
}
