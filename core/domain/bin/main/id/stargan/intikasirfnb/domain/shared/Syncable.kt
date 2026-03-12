package id.stargan.intikasirfnb.domain.shared

enum class SyncStatus {
    PENDING,
    SYNCED,
    CONFLICT
}

data class SyncMetadata(
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val syncVersion: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdByTerminalId: String? = null,
    val updatedByTerminalId: String? = null,
    val deletedAt: Long? = null
) {
    val isDeleted: Boolean get() = deletedAt != null

    fun markSynced(newVersion: Long): SyncMetadata =
        copy(syncStatus = SyncStatus.SYNCED, syncVersion = newVersion)

    fun markUpdated(terminalId: String? = null): SyncMetadata =
        copy(
            syncStatus = SyncStatus.PENDING,
            updatedAt = System.currentTimeMillis(),
            updatedByTerminalId = terminalId
        )

    fun markDeleted(terminalId: String? = null): SyncMetadata =
        copy(
            syncStatus = SyncStatus.PENDING,
            deletedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            updatedByTerminalId = terminalId
        )
}

interface Syncable {
    val syncMetadata: SyncMetadata
}
