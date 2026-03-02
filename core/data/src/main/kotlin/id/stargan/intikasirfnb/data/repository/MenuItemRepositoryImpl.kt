package id.stargan.intikasirfnb.data.repository

import id.stargan.intikasirfnb.data.local.dao.MenuItemDao
import id.stargan.intikasirfnb.data.mapper.toDomain
import id.stargan.intikasirfnb.data.mapper.toEntity
import id.stargan.intikasirfnb.domain.catalog.CategoryId
import id.stargan.intikasirfnb.domain.catalog.MenuItem
import id.stargan.intikasirfnb.domain.catalog.MenuItemRepository
import id.stargan.intikasirfnb.domain.catalog.ProductId
import id.stargan.intikasirfnb.domain.identity.TenantId

class MenuItemRepositoryImpl(private val dao: MenuItemDao) : MenuItemRepository {
    override suspend fun getById(id: ProductId): MenuItem? = dao.getById(id.value)?.toDomain()
    override suspend fun save(menuItem: MenuItem) { dao.insert(menuItem.toEntity()) }
    override suspend fun listByTenant(tenantId: TenantId): List<MenuItem> = dao.listByTenant(tenantId.value).map { it.toDomain() }
    override suspend fun listByCategory(categoryId: CategoryId): List<MenuItem> = dao.listByCategory(categoryId.value).map { it.toDomain() }
}
