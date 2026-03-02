package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.UserEntity

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE tenantId = :tenantId AND email = :email")
    suspend fun getByEmail(tenantId: String, email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: UserEntity)

    @Query("SELECT * FROM users WHERE tenantId = :tenantId ORDER BY displayName")
    suspend fun listByTenant(tenantId: String): List<UserEntity>
}
