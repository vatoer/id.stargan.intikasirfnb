package id.stargan.intikasirfnb.data.repository

import id.stargan.intikasirfnb.data.local.dao.CategoryDao
import id.stargan.intikasirfnb.data.mapper.toDomain
import id.stargan.intikasirfnb.data.mapper.toEntity
import id.stargan.intikasirfnb.domain.catalog.Category
import id.stargan.intikasirfnb.domain.catalog.CategoryId
import id.stargan.intikasirfnb.domain.catalog.CategoryRepository
import id.stargan.intikasirfnb.domain.identity.TenantId

class CategoryRepositoryImpl(private val dao: CategoryDao) : CategoryRepository {
    override suspend fun getById(id: CategoryId): Category? = dao.getById(id.value)?.toDomain()
    override suspend fun save(category: Category) { dao.insert(category.toEntity()) }
    override suspend fun delete(id: CategoryId) { dao.deleteById(id.value) }
    override suspend fun listByTenant(tenantId: TenantId): List<Category> = dao.listByTenant(tenantId.value).map { it.toDomain() }
}
