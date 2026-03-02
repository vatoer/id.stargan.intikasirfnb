package id.stargan.intikasirfnb.domain.usecase.identity

import id.stargan.intikasirfnb.domain.identity.TenantRepository

class CheckOnboardingNeededUseCase(
    private val tenantRepository: TenantRepository
) {
    suspend operator fun invoke(): Boolean {
        return tenantRepository.listAll().isEmpty()
    }
}
