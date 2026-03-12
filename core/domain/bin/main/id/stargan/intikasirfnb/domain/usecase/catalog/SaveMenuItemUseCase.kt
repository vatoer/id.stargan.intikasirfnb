package id.stargan.intikasirfnb.domain.usecase.catalog

import id.stargan.intikasirfnb.domain.catalog.MenuItem
import id.stargan.intikasirfnb.domain.catalog.MenuItemRepository

class SaveMenuItemUseCase(
    private val menuItemRepository: MenuItemRepository
) {
    suspend operator fun invoke(menuItem: MenuItem) {
        menuItemRepository.save(menuItem)
    }
}
