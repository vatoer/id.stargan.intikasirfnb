package id.stargan.intikasirfnb.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "terminal_settings",
    indices = [Index("outletId")]
)
data class TerminalSettingsEntity(
    @PrimaryKey val terminalId: String,
    val outletId: String,
    // Printer
    val printerConnectionType: String = "NONE",
    val printerAddress: String? = null,
    val printerName: String? = null,
    val printerAutoCut: Boolean = true,
    val printerDensity: Int = 5,
    val autoPrintReceipt: Boolean = true,
    val autoPrintKitchenTicket: Boolean = true,
    val receiptCopies: Int = 1,
    val kitchenTicketCopies: Int = 1,
    val openCashDrawer: Boolean = false,
    // Sync metadata
    val syncStatus: String = "PENDING",
    val syncVersion: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdByTerminalId: String? = null,
    val updatedByTerminalId: String? = null,
    val deletedAt: Long? = null
)
