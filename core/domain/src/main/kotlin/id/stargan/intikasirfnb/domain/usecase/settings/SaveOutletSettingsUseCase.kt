package id.stargan.intikasirfnb.domain.usecase.settings

import id.stargan.intikasirfnb.domain.settings.OutletSettings
import id.stargan.intikasirfnb.domain.settings.OutletSettingsRepository
import java.math.BigDecimal

class SaveOutletSettingsUseCase(private val outletSettingsRepository: OutletSettingsRepository) {
    suspend operator fun invoke(settings: OutletSettings) {
        if (settings.serviceCharge.isEnabled) {
            require(settings.serviceCharge.rate > BigDecimal.ZERO) { "Service charge rate must be positive when enabled" }
            require(settings.serviceCharge.rate <= BigDecimal("100")) { "Service charge rate must not exceed 100%" }
        }
        outletSettingsRepository.save(settings)
    }
}
