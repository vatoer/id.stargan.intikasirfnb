package id.stargan.intikasirfnb.data.repository

import id.stargan.intikasirfnb.data.local.dao.MenuItemModifierGroupDao
import id.stargan.intikasirfnb.data.local.dao.ModifierGroupDao
import id.stargan.intikasirfnb.data.local.dao.ModifierOptionDao
import id.stargan.intikasirfnb.data.mapper.toDomain
import id.stargan.intikasirfnb.data.mapper.toEntity
import id.stargan.intikasirfnb.domain.catalog.MenuItemModifierLink
import id.stargan.intikasirfnb.domain.catalog.ModifierGroup
import id.stargan.intikasirfnb.domain.catalog.ModifierGroupId
import id.stargan.intikasirfnb.domain.catalog.ModifierGroupRepository
import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.identity.TenantId

class ModifierGroupRepositoryImpl(
    private val groupDao: ModifierGroupDao,
    private val optionDao: ModifierOptionDao,
    private val linkDao: MenuItemModifierGroupDao
) : ModifierGroupRepository {

    override suspend fun getById(id: ModifierGroupId): ModifierGroup? {
        val entity = groupDao.getById(id.value) ?: return null
        val options = optionDao.listByGroup(id.value).map { it.toDomain() }
        return entity.toDomain(options)
    }

    override suspend fun save(group: ModifierGroup) {
        groupDao.insert(group.toEntity())
        // Replace all options: delete old, insert new
        optionDao.deleteByGroup(group.id.value)
        if (group.options.isNotEmpty()) {
            optionDao.insertAll(group.options.map { it.toEntity() })
        }
    }

    override suspend fun delete(id: ModifierGroupId) {
        // Options cascade-deleted via FK
        groupDao.deleteById(id.value)
    }

    override suspend fun listByTenant(tenantId: TenantId): List<ModifierGroup> {
        val groups = groupDao.listByTenant(tenantId.value)
        return groups.map { entity ->
            val options = optionDao.listByGroup(entity.id).map { it.toDomain() }
            entity.toDomain(options)
        }
    }

    override suspend fun getLinksForItem(menuItemId: ProductId): List<MenuItemModifierLink> =
        linkDao.listByMenuItem(menuItemId.value).map { it.toDomain() }

    override suspend fun saveLink(link: MenuItemModifierLink) {
        linkDao.insert(link.toEntity())
    }

    override suspend fun deleteLink(menuItemId: ProductId, modifierGroupId: ModifierGroupId) {
        linkDao.delete(menuItemId.value, modifierGroupId.value)
    }

    override suspend fun deleteAllLinksForItem(menuItemId: ProductId) {
        linkDao.deleteAllForItem(menuItemId.value)
    }
}
