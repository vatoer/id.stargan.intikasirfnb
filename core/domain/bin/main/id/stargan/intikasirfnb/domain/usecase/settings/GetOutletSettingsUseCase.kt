package id.stargan.intikasirfnb.domain.usecase.settings

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.settings.OutletSettings
import id.stargan.intikasirfnb.domain.settings.OutletSettingsRepository

class GetOutletSettingsUseCase(private val outletSettingsRepository: OutletSettingsRepository) {
    suspend operator fun invoke(outletId: OutletId): OutletSettings? =
        outletSettingsRepository.getByOutletId(outletId)
}
