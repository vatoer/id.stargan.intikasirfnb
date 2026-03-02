package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sales",
    foreignKeys = [ForeignKey(entity = OutletEntity::class, parentColumns = ["id"], childColumns = ["outletId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("outletId"), Index("status"), Index("tableId")]
)
data class SaleEntity(
    @PrimaryKey val id: String,
    val outletId: String,
    val channel: String,
    val tableId: String? = null,
    val externalOrderId: String? = null,
    val cashierId: String? = null,
    val customerId: String? = null,
    val status: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)
