package id.stargan.intikasirfnb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.stargan.intikasirfnb.data.local.entity.KitchenTicketEntity
import id.stargan.intikasirfnb.data.local.entity.KitchenTicketItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KitchenTicketDao {

    @Query("SELECT * FROM kitchen_tickets WHERE id = :id")
    suspend fun getById(id: String): KitchenTicketEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(entity: KitchenTicketEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<KitchenTicketItemEntity>)

    @Query("SELECT * FROM kitchen_ticket_items WHERE ticketId = :ticketId")
    suspend fun getItemsByTicketId(ticketId: String): List<KitchenTicketItemEntity>

    @Query("DELETE FROM kitchen_ticket_items WHERE ticketId = :ticketId")
    suspend fun deleteItemsByTicketId(ticketId: String)

    @Query("SELECT * FROM kitchen_tickets WHERE saleId = :saleId ORDER BY createdAtMillis")
    suspend fun getBySaleId(saleId: String): List<KitchenTicketEntity>

    @Query("""
        SELECT * FROM kitchen_tickets
        WHERE outletId = :outletId AND status IN ('PENDING', 'PREPARING', 'READY')
        ORDER BY createdAtMillis ASC
    """)
    suspend fun getActiveByOutlet(outletId: String): List<KitchenTicketEntity>

    @Query("""
        SELECT * FROM kitchen_tickets
        WHERE outletId = :outletId AND status IN ('PENDING', 'PREPARING', 'READY')
        ORDER BY createdAtMillis ASC
    """)
    fun streamActiveByOutlet(outletId: String): Flow<List<KitchenTicketEntity>>

    @Query("""
        SELECT COALESCE(MAX(ticketNumber), 0) + 1 FROM kitchen_tickets
        WHERE outletId = :outletId
        AND createdAtMillis >= :todayStartMillis
    """)
    suspend fun getNextTicketNumber(outletId: String, todayStartMillis: Long): Int

    @Query("UPDATE kitchen_tickets SET status = :status, startedAtMillis = :startedAt, assignedTo = :assignedTo WHERE id = :id")
    suspend fun updateStartPreparing(id: String, status: String, startedAt: Long, assignedTo: String?)

    @Query("UPDATE kitchen_tickets SET status = :status, readyAtMillis = :readyAt WHERE id = :id")
    suspend fun updateMarkReady(id: String, status: String, readyAt: Long)

    @Query("UPDATE kitchen_tickets SET status = :status, servedAtMillis = :servedAt WHERE id = :id")
    suspend fun updateMarkServed(id: String, status: String, servedAt: Long)
}
