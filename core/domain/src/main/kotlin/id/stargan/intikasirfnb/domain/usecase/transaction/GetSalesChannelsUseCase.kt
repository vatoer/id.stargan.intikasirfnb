package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.transaction.SalesChannel
import id.stargan.intikasirfnb.domain.transaction.SalesChannelRepository

class GetSalesChannelsUseCase(private val repository: SalesChannelRepository) {
    suspend operator fun invoke(tenantId: TenantId): List<SalesChannel> =
        repository.listByTenant(tenantId).filter { it.isActive }
}
