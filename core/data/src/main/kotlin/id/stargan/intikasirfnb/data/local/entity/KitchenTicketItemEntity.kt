package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "kitchen_ticket_items",
    foreignKeys = [
        ForeignKey(
            entity = KitchenTicketEntity::class,
            parentColumns = ["id"],
            childColumns = ["ticketId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ticketId")]
)
data class KitchenTicketItemEntity(
    @PrimaryKey val id: String,
    val ticketId: String,
    val orderLineId: String,
    val productName: String,
    val quantity: Int,
    val modifiers: String? = null,
    val notes: String? = null
)
