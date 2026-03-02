package id.stargan.intikasirfnb.domain.usecase.identity

import id.stargan.intikasirfnb.domain.identity.Outlet
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.identity.OutletRepository

class GetOutletsByTenantUseCase(
    private val outletRepository: OutletRepository
) {
    suspend operator fun invoke(tenantId: TenantId): List<Outlet> =
        outletRepository.listByTenant(tenantId)
}
