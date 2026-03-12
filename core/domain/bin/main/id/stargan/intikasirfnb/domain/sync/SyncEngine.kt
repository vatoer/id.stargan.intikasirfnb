package id.stargan.intikasirfnb.domain.sync

interface SyncEngine {
    suspend fun pushChanges(): SyncResult
    suspend fun pullChanges(): SyncResult
    suspend fun sync(): SyncResult
    fun isEnabled(): Boolean
}

data class SyncResult(
    val success: Boolean,
    val pushed: Int = 0,
    val pulled: Int = 0,
    val conflicts: Int = 0,
    val errorMessage: String? = null
) {
    companion object {
        fun success(pushed: Int = 0, pulled: Int = 0) =
            SyncResult(success = true, pushed = pushed, pulled = pulled)

        fun failure(message: String) =
            SyncResult(success = false, errorMessage = message)

        val NOOP = SyncResult(success = true)
    }
}
