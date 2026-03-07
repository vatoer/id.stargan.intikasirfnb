package id.stargan.intikasirfnb.data.repository

import id.stargan.intikasirfnb.data.local.dao.SalesChannelDao
import id.stargan.intikasirfnb.data.mapper.toDomain
import id.stargan.intikasirfnb.data.mapper.toEntity
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.transaction.SalesChannel
import id.stargan.intikasirfnb.domain.transaction.SalesChannelId
import id.stargan.intikasirfnb.domain.transaction.SalesChannelRepository

class SalesChannelRepositoryImpl(
    private val dao: SalesChannelDao
) : SalesChannelRepository {

    override suspend fun getById(id: SalesChannelId): SalesChannel? =
        dao.getById(id.value)?.toDomain()

    override suspend fun save(channel: SalesChannel) {
        dao.insert(channel.toEntity())
    }

    override suspend fun listByTenant(tenantId: TenantId): List<SalesChannel> =
        dao.listByTenant(tenantId.value).map { it.toDomain() }

    override suspend fun delete(id: SalesChannelId) {
        dao.delete(id.value)
    }
}
