package id.stargan.intikasirfnb.domain.usecase.settings

import id.stargan.intikasirfnb.domain.settings.TenantSettings
import id.stargan.intikasirfnb.domain.settings.TenantSettingsRepository

class SaveTenantSettingsUseCase(private val tenantSettingsRepository: TenantSettingsRepository) {
    suspend operator fun invoke(settings: TenantSettings) {
        require(settings.defaultCurrencyCode.length == 3) { "Currency code must be 3 characters" }
        tenantSettingsRepository.save(settings)
    }
}
