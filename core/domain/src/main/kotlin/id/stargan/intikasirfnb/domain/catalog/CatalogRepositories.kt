package id.stargan.intikasirfnb.domain.catalog

import id.stargan.intikasirfnb.domain.identity.TenantId

interface CategoryRepository {
    suspend fun getById(id: CategoryId): Category?
    suspend fun save(category: Category)
    suspend fun listByTenant(tenantId: TenantId): List<Category>
}

interface MenuItemRepository {
    suspend fun getById(id: ProductId): MenuItem?
    suspend fun save(menuItem: MenuItem)
    suspend fun listByTenant(tenantId: TenantId): List<MenuItem>
    suspend fun listByCategory(categoryId: CategoryId): List<MenuItem>
}
