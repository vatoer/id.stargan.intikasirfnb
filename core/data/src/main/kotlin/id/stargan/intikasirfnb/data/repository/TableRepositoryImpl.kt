package id.stargan.intikasirfnb.data.repository

import id.stargan.intikasirfnb.data.local.dao.TableDao
import id.stargan.intikasirfnb.data.mapper.toDomain
import id.stargan.intikasirfnb.data.mapper.toEntity
import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.transaction.SaleId
import id.stargan.intikasirfnb.domain.transaction.Table
import id.stargan.intikasirfnb.domain.transaction.TableId
import id.stargan.intikasirfnb.domain.transaction.TableRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TableRepositoryImpl(
    private val dao: TableDao
) : TableRepository {
    override suspend fun getById(id: TableId): Table? = dao.getById(id.value)?.toDomain()

    override suspend fun save(table: Table) { dao.insert(table.toEntity()) }

    override suspend fun delete(id: TableId) { dao.softDelete(id.value) }

    override suspend fun listByOutlet(outletId: OutletId): List<Table> =
        dao.listByOutlet(outletId.value).map { it.toDomain() }

    override fun streamByOutlet(outletId: OutletId): Flow<List<Table>> =
        dao.streamByOutlet(outletId.value).map { list -> list.map { it.toDomain() } }

    override suspend fun listAvailable(outletId: OutletId): List<Table> =
        dao.listAvailable(outletId.value).map { it.toDomain() }

    override suspend fun listOccupied(outletId: OutletId): List<Table> =
        dao.listOccupied(outletId.value).map { it.toDomain() }

    override suspend fun findBySaleId(saleId: SaleId): Table? =
        dao.findBySaleId(saleId.value)?.toDomain()

    override suspend fun occupyTable(tableId: TableId, saleId: SaleId) {
        dao.updateCurrentSaleId(tableId.value, saleId.value)
    }

    override suspend fun releaseTable(tableId: TableId) {
        dao.updateCurrentSaleId(tableId.value, null)
    }

    override suspend fun releaseBySaleId(saleId: SaleId) {
        dao.releaseBySaleId(saleId.value)
    }

    override suspend fun listSections(outletId: OutletId): List<String> =
        dao.listSections(outletId.value)
}
