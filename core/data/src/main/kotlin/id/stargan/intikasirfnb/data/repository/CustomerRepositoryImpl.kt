package id.stargan.intikasirfnb.data.repository

import id.stargan.intikasirfnb.data.local.dao.CustomerDao
import id.stargan.intikasirfnb.data.mapper.toDomain
import id.stargan.intikasirfnb.data.mapper.toEntity
import id.stargan.intikasirfnb.domain.customer.Customer
import id.stargan.intikasirfnb.domain.customer.CustomerId
import id.stargan.intikasirfnb.domain.customer.CustomerRepository
import id.stargan.intikasirfnb.domain.identity.TenantId

class CustomerRepositoryImpl(
    private val dao: CustomerDao
) : CustomerRepository {
    override suspend fun getById(id: CustomerId): Customer? = dao.getById(id.value)?.toDomain()
    override suspend fun save(customer: Customer) { dao.insert(customer.toEntity()) }
    override suspend fun delete(id: CustomerId) { dao.deleteById(id.value) }
    override suspend fun listByTenant(tenantId: TenantId): List<Customer> = dao.listByTenant(tenantId.value).map { it.toDomain() }
    override suspend fun search(tenantId: TenantId, query: String): List<Customer> = dao.search(tenantId.value, query).map { it.toDomain() }
}
