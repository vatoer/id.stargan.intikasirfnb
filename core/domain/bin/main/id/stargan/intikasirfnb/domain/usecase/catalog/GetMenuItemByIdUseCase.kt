package id.stargan.intikasirfnb.domain.usecase.catalog

import id.stargan.intikasirfnb.domain.catalog.MenuItem
import id.stargan.intikasirfnb.domain.catalog.MenuItemRepository
import id.stargan.intikasirfnb.domain.catalog.ProductId

class GetMenuItemByIdUseCase(
    private val menuItemRepository: MenuItemRepository
) {
    suspend operator fun invoke(productId: ProductId): MenuItem? =
        menuItemRepository.getById(productId)
}
