package id.stargan.intikasirfnb.domain.usecase.identity

import id.stargan.intikasirfnb.domain.identity.Tenant
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.identity.TenantRepository

class GetTenantUseCase(
    private val tenantRepository: TenantRepository
) {
    suspend operator fun invoke(tenantId: TenantId): Tenant? = tenantRepository.getById(tenantId)
}
