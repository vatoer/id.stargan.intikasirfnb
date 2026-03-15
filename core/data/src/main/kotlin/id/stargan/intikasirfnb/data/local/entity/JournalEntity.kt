package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "journals",
    indices = [Index("outletId"), Index("createdAtMillis"), Index("referenceType", "referenceId")]
)
data class JournalEntity(
    @PrimaryKey val id: String,
    val outletId: String,
    val description: String? = null,
    val referenceType: String? = null,
    val referenceId: String? = null,
    val entriesJson: String, // JSON array of journal entries
    val createdAtMillis: Long,
    // Sync metadata
    val syncStatus: String = "PENDING",
    val syncVersion: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdByTerminalId: String? = null,
    val updatedByTerminalId: String? = null,
    val deletedAt: Long? = null
)

@Entity(
    tableName = "accounts",
    indices = [Index("code", unique = true)]
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val code: String,
    val name: String,
    val type: String // AccountType enum name
)
