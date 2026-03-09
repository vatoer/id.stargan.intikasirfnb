package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "kitchen_tickets",
    indices = [
        Index("saleId"),
        Index("outletId", "status"),
        Index("outletId", "createdAtMillis")
    ]
)
data class KitchenTicketEntity(
    @PrimaryKey val id: String,
    val saleId: String,
    val outletId: String,
    val station: String = "GENERAL", // KITCHEN, BAR, GENERAL
    val status: String = "PENDING",  // PENDING, PREPARING, READY, SERVED
    val assignedTo: String? = null,
    val tableName: String? = null,
    val channelName: String? = null,
    val ticketNumber: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val startedAtMillis: Long? = null,
    val readyAtMillis: Long? = null,
    val servedAtMillis: Long? = null,
    // Sync metadata
    val syncStatus: String = "PENDING",
    val syncVersion: Long = 0L,
    val createdByTerminalId: String? = null,
    val updatedByTerminalId: String? = null
)
