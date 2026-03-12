package id.stargan.intikasirfnb.domain.usecase.catalog

import id.stargan.intikasirfnb.domain.catalog.CategoryId
import id.stargan.intikasirfnb.domain.catalog.MenuItem
import id.stargan.intikasirfnb.domain.catalog.MenuItemRepository

class GetMenuItemsByCategoryUseCase(private val menuItemRepository: MenuItemRepository) {
    suspend operator fun invoke(categoryId: CategoryId): List<MenuItem> =
        menuItemRepository.listByCategory(categoryId)
}
