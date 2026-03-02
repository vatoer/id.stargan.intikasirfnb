package id.stargan.intikasirfnb.data.repository

import id.stargan.intikasirfnb.data.local.dao.UserDao
import id.stargan.intikasirfnb.data.mapper.toDomain
import id.stargan.intikasirfnb.data.mapper.toEntity
import id.stargan.intikasirfnb.domain.identity.TenantId
import id.stargan.intikasirfnb.domain.identity.User
import id.stargan.intikasirfnb.domain.identity.UserId
import id.stargan.intikasirfnb.domain.identity.UserRepository

class UserRepositoryImpl(
    private val dao: UserDao
) : UserRepository {
    override suspend fun getById(id: UserId): User? = dao.getById(id.value)?.toDomain()
    override suspend fun getByEmail(tenantId: TenantId, email: String): User? = dao.getByEmail(tenantId.value, email)?.toDomain()
    override suspend fun save(user: User) { dao.insert(user.toEntity()) }
    override suspend fun listByTenant(tenantId: TenantId): List<User> = dao.listByTenant(tenantId.value).map { it.toDomain() }
}
