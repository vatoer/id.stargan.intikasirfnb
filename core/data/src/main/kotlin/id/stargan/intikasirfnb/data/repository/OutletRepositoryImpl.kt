package id.stargan.intikasirfnb.data.repository

import id.stargan.intikasirfnb.data.local.dao.OutletDao
import id.stargan.intikasirfnb.data.mapper.toDomain
import id.stargan.intikasirfnb.data.mapper.toEntity
import id.stargan.intikasirfnb.domain.identity.Outlet
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.identity.OutletRepository
import id.stargan.intikasirfnb.domain.identity.TenantId

class OutletRepositoryImpl(
    private val dao: OutletDao
) : OutletRepository {
    override suspend fun getById(id: OutletId): Outlet? = dao.getById(id.value)?.toDomain()
    override suspend fun save(outlet: Outlet) { dao.insert(outlet.toEntity()) }
    override suspend fun listByTenant(tenantId: TenantId): List<Outlet> = dao.listByTenant(tenantId.value).map { it.toDomain() }
}
