package id.stargan.intikasirfnb.data.repository

import id.stargan.intikasirfnb.data.local.dao.AddOnGroupDao
import id.stargan.intikasirfnb.data.local.dao.AddOnItemDao
import id.stargan.intikasirfnb.data.local.dao.MenuItemAddOnGroupDao
import id.stargan.intikasirfnb.data.mapper.toDomain
import id.stargan.intikasirfnb.data.mapper.toEntity
import id.stargan.intikasirfnb.domain.catalog.AddOnGroup
import id.stargan.intikasirfnb.domain.catalog.AddOnGroupId
import id.stargan.intikasirfnb.domain.catalog.AddOnGroupRepository
import id.stargan.intikasirfnb.domain.catalog.MenuItemAddOnLink
import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.identity.TenantId

class AddOnGroupRepositoryImpl(
    private val groupDao: AddOnGroupDao,
    private val itemDao: AddOnItemDao,
    private val linkDao: MenuItemAddOnGroupDao
) : AddOnGroupRepository {

    override suspend fun getById(id: AddOnGroupId): AddOnGroup? {
        val entity = groupDao.getById(id.value) ?: return null
        val items = itemDao.listByGroup(id.value).map { it.toDomain() }
        return entity.toDomain(items)
    }

    override suspend fun save(group: AddOnGroup) {
        groupDao.insert(group.toEntity())
        // Replace all items: delete old, insert new
        itemDao.deleteByGroup(group.id.value)
        if (group.items.isNotEmpty()) {
            itemDao.insertAll(group.items.map { it.toEntity() })
        }
    }

    override suspend fun delete(id: AddOnGroupId) {
        // Items cascade-deleted via FK
        groupDao.deleteById(id.value)
    }

    override suspend fun listByTenant(tenantId: TenantId): List<AddOnGroup> {
        val groups = groupDao.listByTenant(tenantId.value)
        return groups.map { entity ->
            val items = itemDao.listByGroup(entity.id).map { it.toDomain() }
            entity.toDomain(items)
        }
    }

    override suspend fun getLinksForItem(menuItemId: ProductId): List<MenuItemAddOnLink> =
        linkDao.listByMenuItem(menuItemId.value).map { it.toDomain() }

    override suspend fun saveLink(link: MenuItemAddOnLink) {
        linkDao.insert(link.toEntity())
    }

    override suspend fun deleteLink(menuItemId: ProductId, addOnGroupId: AddOnGroupId) {
        linkDao.delete(menuItemId.value, addOnGroupId.value)
    }

    override suspend fun deleteAllLinksForItem(menuItemId: ProductId) {
        linkDao.deleteAllForItem(menuItemId.value)
    }
}
