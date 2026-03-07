package id.stargan.intikasirfnb.domain.usecase.catalog

import id.stargan.intikasirfnb.domain.catalog.ModifierGroup
import id.stargan.intikasirfnb.domain.catalog.ModifierGroupRepository
import id.stargan.intikasirfnb.domain.identity.TenantId

class GetModifierGroupsUseCase(
    private val repository: ModifierGroupRepository
) {
    suspend operator fun invoke(tenantId: TenantId): List<ModifierGroup> =
        repository.listByTenant(tenantId)
}
