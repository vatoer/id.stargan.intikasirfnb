package id.stargan.intikasirfnb.domain.usecase.catalog

import id.stargan.intikasirfnb.domain.catalog.MenuItem
import id.stargan.intikasirfnb.domain.catalog.MenuItemRepository
import id.stargan.intikasirfnb.domain.identity.TenantId

class SearchMenuItemsUseCase(
    private val repository: MenuItemRepository
) {
    suspend operator fun invoke(tenantId: TenantId, query: String): List<MenuItem> =
        repository.searchByName(tenantId, query)
}
