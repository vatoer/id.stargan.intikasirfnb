package id.stargan.intikasirfnb.domain.usecase.transaction

import id.stargan.intikasirfnb.domain.identity.OutletId
import id.stargan.intikasirfnb.domain.transaction.Table
import id.stargan.intikasirfnb.domain.transaction.TableRepository

class GetTablesByOutletUseCase(private val tableRepository: TableRepository) {
    suspend operator fun invoke(outletId: OutletId): List<Table> =
        tableRepository.listByOutlet(outletId)
}
