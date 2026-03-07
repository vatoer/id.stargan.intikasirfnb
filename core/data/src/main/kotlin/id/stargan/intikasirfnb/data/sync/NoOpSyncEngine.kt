package id.stargan.intikasirfnb.data.sync

import id.stargan.intikasirfnb.domain.sync.SyncEngine
import id.stargan.intikasirfnb.domain.sync.SyncResult
import javax.inject.Inject

class NoOpSyncEngine @Inject constructor() : SyncEngine {
    override suspend fun pushChanges(): SyncResult = SyncResult.NOOP
    override suspend fun pullChanges(): SyncResult = SyncResult.NOOP
    override suspend fun sync(): SyncResult = SyncResult.NOOP
    override fun isEnabled(): Boolean = false
}
