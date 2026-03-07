package id.stargan.intikasirfnb.domain.usecase.catalog

import id.stargan.intikasirfnb.domain.catalog.MenuItemRepository
import id.stargan.intikasirfnb.domain.catalog.ModifierGroupRepository
import id.stargan.intikasirfnb.domain.catalog.ProductId

class DeleteMenuItemUseCase(
    private val menuItemRepository: MenuItemRepository,
    private val modifierGroupRepository: ModifierGroupRepository
) {
    suspend operator fun invoke(id: ProductId) {
        modifierGroupRepository.deleteAllLinksForItem(id)
        menuItemRepository.delete(id)
    }
}
