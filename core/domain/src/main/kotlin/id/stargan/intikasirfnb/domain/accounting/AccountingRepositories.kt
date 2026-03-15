package id.stargan.intikasirfnb.domain.accounting

import id.stargan.intikasirfnb.domain.identity.OutletId

interface JournalRepository {
    suspend fun save(journal: Journal)
    suspend fun getById(id: JournalId): Journal?
    suspend fun listByOutlet(outletId: OutletId, limit: Int = 100): List<Journal>
    suspend fun listByDateRange(outletId: OutletId, fromMillis: Long, toMillis: Long): List<Journal>
}

interface AccountRepository {
    suspend fun getById(id: AccountId): Account?
    suspend fun save(account: Account)
    suspend fun listAll(): List<Account>
}
