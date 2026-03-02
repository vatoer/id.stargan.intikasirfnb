package id.stargan.intikasirfnb.domain.usecase.customer

import id.stargan.intikasirfnb.domain.customer.Customer
import id.stargan.intikasirfnb.domain.customer.CustomerRepository
import id.stargan.intikasirfnb.domain.identity.TenantId

class GetCustomersUseCase(private val customerRepository: CustomerRepository) {
    suspend operator fun invoke(tenantId: TenantId): List<Customer> =
        customerRepository.listByTenant(tenantId)
}
