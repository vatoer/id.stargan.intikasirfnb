package id.stargan.intikasirfnb.domain.accounting

interface JournalRepository {
    suspend fun save(journal: Journal)
    suspend fun getById(id: JournalId): Journal?
}

interface AccountRepository {
    suspend fun getById(id: AccountId): Account?
    suspend fun listAll(): List<Account>
}
