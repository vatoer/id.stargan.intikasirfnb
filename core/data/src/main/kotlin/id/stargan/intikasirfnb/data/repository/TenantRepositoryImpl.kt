package id.stargan.intikasirfnb.data.repository

import id.stargan.intikasirfnb.data.local.dao.TenantDao
import id.stargan.intikasirfnb.data.mapper.toDomain
import id.stargan.intikasirfnb.data.mapper.toEntity
import id.stargan.intikasirfnb.domain.identity.Tenant
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.identity.TenantRepository

class TenantRepositoryImpl(private val dao: TenantDao) : TenantRepository {
    override suspend fun getById(id: TenantId): Tenant? = dao.getById(id.value)?.toDomain()
    override suspend fun save(tenant: Tenant) { dao.insert(tenant.toEntity()) }
    override suspend fun listAll(): List<Tenant> = dao.listAll().map { it.toDomain() }
}
