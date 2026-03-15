package id.stargan.intikasirfnb.data.repository

import id.stargan.intikasirfnb.data.local.dao.AccountDao
import id.stargan.intikasirfnb.data.mapper.toDomain
import id.stargan.intikasirfnb.data.mapper.toEntity
import id.stargan.intikasirfnb.domain.accounting.Account
import id.stargan.intikasirfnb.domain.accounting.AccountId
import id.stargan.intikasirfnb.domain.accounting.AccountRepository

class AccountRepositoryImpl(private val dao: AccountDao) : AccountRepository {
    override suspend fun getById(id: AccountId) = dao.getById(id.value)?.toDomain()
    override suspend fun save(account: Account) = dao.insert(account.toEntity())
    override suspend fun listAll() = dao.listAll().map { it.toDomain() }
}
