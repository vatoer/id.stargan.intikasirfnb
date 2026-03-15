package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.CustomerEntity

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getById(id: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CustomerEntity)

    @Query("SELECT * FROM customers WHERE tenantId = :tenantId ORDER BY name")
    suspend fun listByTenant(tenantId: String): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE tenantId = :tenantId AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%') ORDER BY name")
    suspend fun search(tenantId: String, query: String): List<CustomerEntity>

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteById(id: String)
}
