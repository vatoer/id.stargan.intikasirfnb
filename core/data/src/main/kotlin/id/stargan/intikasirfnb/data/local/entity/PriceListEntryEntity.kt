package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "price_list_entries",
    primaryKeys = ["priceListId", "productId"],
    foreignKeys = [
        ForeignKey(
            entity = PriceListEntity::class,
            parentColumns = ["id"],
            childColumns = ["priceListId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("priceListId"), Index("productId")]
)
data class PriceListEntryEntity(
    val priceListId: String,
    val productId: String,
    val priceAmount: String,
    val priceCurrency: String = "IDR"
)
