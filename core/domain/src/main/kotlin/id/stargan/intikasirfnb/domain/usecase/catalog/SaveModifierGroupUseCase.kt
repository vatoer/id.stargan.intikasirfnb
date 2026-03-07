package id.stargan.intikasirfnb.domain.usecase.catalog

import id.stargan.intikasirfnb.domain.catalog.ModifierGroup
import id.stargan.intikasirfnb.domain.catalog.ModifierGroupRepository

class SaveModifierGroupUseCase(
    private val repository: ModifierGroupRepository
) {
    suspend operator fun invoke(group: ModifierGroup) {
        require(group.name.isNotBlank()) { "Nama modifier group harus diisi" }
        repository.save(group)
    }
}
