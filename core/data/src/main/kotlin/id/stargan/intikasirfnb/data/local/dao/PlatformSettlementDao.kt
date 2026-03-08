package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.PlatformSettlementEntity

@Dao
interface PlatformSettlementDao {
    @Query("SELECT * FROM platform_settlements WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): PlatformSettlementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PlatformSettlementEntity)

    @Query("SELECT * FROM platform_settlements WHERE outletId = :outletId AND deletedAt IS NULL ORDER BY createdAtMillis DESC LIMIT :limit")
    suspend fun listByOutlet(outletId: String, limit: Int = 50): List<PlatformSettlementEntity>

    @Query("SELECT * FROM platform_settlements WHERE channelId = :channelId AND deletedAt IS NULL ORDER BY createdAtMillis DESC LIMIT :limit")
    suspend fun listByChannel(channelId: String, limit: Int = 50): List<PlatformSettlementEntity>

    @Query("SELECT * FROM platform_settlements WHERE outletId = :outletId AND status = :status AND deletedAt IS NULL ORDER BY createdAtMillis DESC")
    suspend fun listByStatus(outletId: String, status: String): List<PlatformSettlementEntity>

    @Query("SELECT * FROM platform_settlements WHERE outletId = :outletId AND status = 'PENDING' AND deletedAt IS NULL ORDER BY createdAtMillis ASC")
    suspend fun listPending(outletId: String): List<PlatformSettlementEntity>

    // --- Aggregate queries for reconciliation ---

    @Query("""
        SELECT COALESCE(SUM(CAST(expectedAmountAmount AS REAL)), 0)
        FROM platform_settlements
        WHERE outletId = :outletId AND status = 'PENDING' AND deletedAt IS NULL
    """)
    suspend fun totalPendingAmount(outletId: String): Double

    @Query("""
        SELECT COALESCE(SUM(CAST(expectedAmountAmount AS REAL)), 0)
        FROM platform_settlements
        WHERE channelId = :channelId AND status = 'PENDING' AND deletedAt IS NULL
    """)
    suspend fun totalPendingAmountByChannel(channelId: String): Double

    @Query("""
        SELECT COALESCE(SUM(CAST(settledAmountAmount AS REAL)), 0)
        FROM platform_settlements
        WHERE outletId = :outletId AND status IN ('SETTLED', 'PARTIAL')
        AND settlementDate BETWEEN :fromMillis AND :toMillis AND deletedAt IS NULL
    """)
    suspend fun totalSettledAmountInRange(outletId: String, fromMillis: Long, toMillis: Long): Double

    @Query("""
        SELECT COALESCE(SUM(CAST(commissionTotalAmount AS REAL)), 0)
        FROM platform_settlements
        WHERE outletId = :outletId AND status IN ('SETTLED', 'PARTIAL')
        AND settlementDate BETWEEN :fromMillis AND :toMillis AND deletedAt IS NULL
    """)
    suspend fun totalCommissionInRange(outletId: String, fromMillis: Long, toMillis: Long): Double

    @Query("SELECT COUNT(*) FROM platform_settlements WHERE outletId = :outletId AND status = 'PENDING' AND deletedAt IS NULL")
    suspend fun countPending(outletId: String): Int

    @Query("""
        SELECT * FROM platform_settlements
        WHERE outletId = :outletId
        AND createdAtMillis BETWEEN :fromMillis AND :toMillis
        AND deletedAt IS NULL
        ORDER BY createdAtMillis DESC
    """)
    suspend fun listByDateRange(outletId: String, fromMillis: Long, toMillis: Long): List<PlatformSettlementEntity>

    @Query("""
        SELECT * FROM platform_settlements
        WHERE channelId = :channelId AND status = 'PENDING' AND deletedAt IS NULL
        ORDER BY createdAtMillis ASC
    """)
    suspend fun listPendingByChannel(channelId: String): List<PlatformSettlementEntity>
}
