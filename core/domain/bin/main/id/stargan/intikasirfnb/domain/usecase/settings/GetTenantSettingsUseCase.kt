package id.stargan.intikasirfnb.domain.usecase.settings

import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.settings.TenantSettings
import id.stargan.intikasirfnb.domain.settings.TenantSettingsRepository

class GetTenantSettingsUseCase(private val tenantSettingsRepository: TenantSettingsRepository) {
    suspend operator fun invoke(tenantId: TenantId): TenantSettings? =
        tenantSettingsRepository.getByTenantId(tenantId)
}
