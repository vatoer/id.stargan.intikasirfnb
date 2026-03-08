package id.stargan.intikasirfnb.data.repository

import id.stargan.intikasirfnb.data.local.dao.PriceListDao
import id.stargan.intikasirfnb.data.mapper.toDomain
import id.stargan.intikasirfnb.data.mapper.toEntity
import id.stargan.intikasirfnb.domain.catalog.PriceList
import id.stargan.intikasirfnb.domain.catalog.PriceListEntry
import id.stargan.intikasirfnb.domain.catalog.PriceListId
import id.stargan.intikasirfnb.domain.catalog.PriceListRepository
import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.identity.TenantId

class PriceListRepositoryImpl(
    private val dao: PriceListDao
) : PriceListRepository {

    override suspend fun getById(id: PriceListId): PriceList? {
        val entity = dao.getById(id.value) ?: return null
        val entries = dao.getEntries(id.value)
        return entity.toDomain(entries)
    }

    override suspend fun save(priceList: PriceList) {
        val entity = priceList.toEntity()
        val entryEntities = priceList.entries.map { it.toEntity(priceList.id.value) }
        dao.savePriceListWithEntries(entity, entryEntities)
    }

    override suspend fun delete(id: PriceListId) {
        dao.delete(id.value)
    }

    override suspend fun listByTenant(tenantId: TenantId): List<PriceList> {
        return dao.listByTenant(tenantId.value).map { entity ->
            val entries = dao.getEntries(entity.id)
            entity.toDomain(entries)
        }
    }

    override suspend fun getPrice(priceListId: PriceListId, productId: ProductId): PriceListEntry? {
        return dao.getEntry(priceListId.value, productId.value)?.toDomain()
    }
}
