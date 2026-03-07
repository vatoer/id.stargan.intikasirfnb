package id.stargan.intikasirfnb.domain.usecase.catalog

import id.stargan.intikasirfnb.domain.catalog.ModifierGroupId
import id.stargan.intikasirfnb.domain.catalog.ModifierGroupRepository

class DeleteModifierGroupUseCase(
    private val repository: ModifierGroupRepository
) {
    suspend operator fun invoke(id: ModifierGroupId) {
        repository.delete(id)
    }
}
