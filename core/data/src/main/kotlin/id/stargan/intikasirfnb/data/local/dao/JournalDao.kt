package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.AccountEntity
import id.stargan.intikasirfnb.data.local.entity.JournalEntity

@Dao
interface JournalDao {
    @Query("SELECT * FROM journals WHERE id = :id")
    suspend fun getById(id: String): JournalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: JournalEntity)

    @Query("SELECT * FROM journals WHERE outletId = :outletId ORDER BY createdAtMillis DESC LIMIT :limit")
    suspend fun listByOutlet(outletId: String, limit: Int): List<JournalEntity>

    @Query("SELECT * FROM journals WHERE outletId = :outletId AND createdAtMillis BETWEEN :fromMillis AND :toMillis ORDER BY createdAtMillis DESC")
    suspend fun listByDateRange(outletId: String, fromMillis: Long, toMillis: Long): List<JournalEntity>
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AccountEntity)

    @Query("SELECT * FROM accounts ORDER BY code")
    suspend fun listAll(): List<AccountEntity>
}
