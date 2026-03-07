package id.stargan.intikasirfnb.data.repository

import id.stargan.intikasirfnb.data.local.dao.TaxConfigDao
import id.stargan.intikasirfnb.data.mapper.toDomain
import id.stargan.intikasirfnb.data.mapper.toEntity
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.settings.TaxConfig
import id.stargan.intikasirfnb.domain.settings.TaxConfigId
import id.stargan.intikasirfnb.domain.settings.TaxConfigRepository

class TaxConfigRepositoryImpl(private val dao: TaxConfigDao) : TaxConfigRepository {
    override suspend fun getById(id: TaxConfigId): TaxConfig? = dao.getById(id.value)?.toDomain()
    override suspend fun getActiveByTenant(tenantId: TenantId): List<TaxConfig> = dao.getActiveByTenant(tenantId.value).map { it.toDomain() }
    override suspend fun getAllByTenant(tenantId: TenantId): List<TaxConfig> = dao.getAllByTenant(tenantId.value).map { it.toDomain() }
    override suspend fun save(taxConfig: TaxConfig) { dao.insert(taxConfig.toEntity()) }
    override suspend fun delete(id: TaxConfigId) { dao.deleteById(id.value) }
}
