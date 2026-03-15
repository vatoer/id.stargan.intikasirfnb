package id.stargan.intikasirfnb.domain.customer

import id.stargan.intikasirfnb.domain.identity.TenantId

interface CustomerRepository {
    suspend fun getById(id: CustomerId): Customer?
    suspend fun save(customer: Customer)
    suspend fun delete(id: CustomerId)
    suspend fun listByTenant(tenantId: TenantId): List<Customer>
    suspend fun search(tenantId: TenantId, query: String): List<Customer>
}
