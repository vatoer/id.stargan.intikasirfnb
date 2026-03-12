package id.stargan.intikasirfnb.domain.usecase.settings

import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.settings.TaxConfig
import id.stargan.intikasirfnb.domain.settings.TaxConfigRepository

class GetActiveTaxConfigsUseCase(private val taxConfigRepository: TaxConfigRepository) {
    suspend operator fun invoke(tenantId: TenantId): List<TaxConfig> =
        taxConfigRepository.getActiveByTenant(tenantId)
}
