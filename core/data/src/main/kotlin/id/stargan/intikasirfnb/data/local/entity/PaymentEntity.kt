package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    foreignKeys = [ForeignKey(entity = SaleEntity::class, parentColumns = ["id"], childColumns = ["saleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("saleId")]
)
data class PaymentEntity(
    @PrimaryKey val id: String,
    val saleId: String,
    val method: String,
    val amountAmount: String,
    val amountCurrency: String = "IDR",
    val reference: String? = null
)
