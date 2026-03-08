package id.stargan.intikasirfnb.domain.catalog

import id.stargan.intikasirfnb.domain.identity.TenantId

interface CategoryRepository {
    suspend fun getById(id: CategoryId): Category?
    suspend fun save(category: Category)
    suspend fun delete(id: CategoryId)
    suspend fun listByTenant(tenantId: TenantId): List<Category>
}

interface MenuItemRepository {
    suspend fun getById(id: ProductId): MenuItem?
    suspend fun save(menuItem: MenuItem)
    suspend fun delete(id: ProductId)
    suspend fun listByTenant(tenantId: TenantId): List<MenuItem>
    suspend fun listByCategory(categoryId: CategoryId): List<MenuItem>
    suspend fun searchByName(tenantId: TenantId, query: String): List<MenuItem>
}

interface PriceListRepository {
    suspend fun getById(id: PriceListId): PriceList?
    suspend fun save(priceList: PriceList)
    suspend fun delete(id: PriceListId)
    suspend fun listByTenant(tenantId: TenantId): List<PriceList>
    /** Get just the price for a specific product in a specific price list (optimized lookup) */
    suspend fun getPrice(priceListId: PriceListId, productId: ProductId): PriceListEntry?
}

interface ModifierGroupRepository {
    suspend fun getById(id: ModifierGroupId): ModifierGroup?
    suspend fun save(group: ModifierGroup)
    suspend fun delete(id: ModifierGroupId)
    suspend fun listByTenant(tenantId: TenantId): List<ModifierGroup>
    suspend fun getLinksForItem(menuItemId: ProductId): List<MenuItemModifierLink>
    suspend fun saveLink(link: MenuItemModifierLink)
    suspend fun deleteLink(menuItemId: ProductId, modifierGroupId: ModifierGroupId)
    suspend fun deleteAllLinksForItem(menuItemId: ProductId)
}
