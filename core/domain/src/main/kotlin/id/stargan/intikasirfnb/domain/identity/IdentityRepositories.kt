package id.stargan.intikasirfnb.domain.identity

interface TenantRepository {
    suspend fun getById(id: TenantId): Tenant?
    suspend fun save(tenant: Tenant)
    suspend fun listAll(): List<Tenant>
}

interface OutletRepository {
    suspend fun getById(id: OutletId): Outlet?
    suspend fun save(outlet: Outlet)
    suspend fun listByTenant(tenantId: TenantId): List<Outlet>
}

interface UserRepository {
    suspend fun getById(id: UserId): User?
    suspend fun getByEmail(tenantId: TenantId, email: String): User?
    suspend fun save(user: User)
    suspend fun listByTenant(tenantId: TenantId): List<User>
}
