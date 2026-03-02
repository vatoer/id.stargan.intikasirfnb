package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tables",
    foreignKeys = [ForeignKey(entity = OutletEntity::class, parentColumns = ["id"], childColumns = ["outletId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("outletId")]
)
data class TableEntity(
    @PrimaryKey val id: String,
    val outletId: String,
    val name: String,
    val capacity: Int = 4,
    val currentSaleId: String? = null,
    val isActive: Boolean = true
)
