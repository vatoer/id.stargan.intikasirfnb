package id.stargan.intikasirfnb.data.repository

import id.stargan.intikasirfnb.data.local.dao.TableDao
import id.stargan.intikasirfnb.data.mapper.toDomain
import id.stargan.intikasirfnb.data.mapper.toEntity
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.transaction.Table
import id.stargan.intikasirfnb.domain.transaction.TableId
import id.stargan.intikasirfnb.domain.transaction.TableRepository

class TableRepositoryImpl(
    private val dao: TableDao
) : TableRepository {
    override suspend fun getById(id: TableId): Table? = dao.getById(id.value)?.toDomain()
    override suspend fun save(table: Table) { dao.insert(table.toEntity()) }
    override suspend fun listByOutlet(outletId: OutletId): List<Table> = dao.listByOutlet(outletId.value).map { it.toDomain() }
    override suspend fun getTableWithActiveSale(tableId: TableId): Table? = dao.getById(tableId.value)?.toDomain()
}
