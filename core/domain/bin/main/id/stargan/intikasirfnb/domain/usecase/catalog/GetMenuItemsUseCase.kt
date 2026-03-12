package id.stargan.intikasirfnb.domain.usecase.catalog

import id.stargan.intikasirfnb.domain.catalog.MenuItem
import id.stargan.intikasirfnb.domain.catalog.MenuItemRepository
import id.stargan.intikasirfnb.domain.identity.TenantId

class GetMenuItemsUseCase(
    private val menuItemRepository: MenuItemRepository
) {
    suspend operator fun invoke(tenantId: TenantId): List<MenuItem> =
        menuItemRepository.listByTenant(tenantId)
}
