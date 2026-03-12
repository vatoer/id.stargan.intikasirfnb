package id.stargan.intikasirfnb.domain.usecase.settings

import id.stargan.intikasirfnb.domain.settings.TaxConfig
import id.stargan.intikasirfnb.domain.settings.TaxConfigRepository
import java.math.BigDecimal

class SaveTaxConfigUseCase(private val taxConfigRepository: TaxConfigRepository) {
    suspend operator fun invoke(taxConfig: TaxConfig) {
        require(taxConfig.name.isNotBlank()) { "Tax name must not be blank" }
        require(taxConfig.rate > BigDecimal.ZERO) { "Tax rate must be positive" }
        require(taxConfig.rate <= BigDecimal("100")) { "Tax rate must not exceed 100%" }
        taxConfigRepository.save(taxConfig)
    }
}
