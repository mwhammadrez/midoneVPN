package com.example.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SyncManager(private val api: SyncApi) {

    // BUG: this scope is created here and never cancelled by anything.
    // Every SyncManager instance leaks a live coroutine scope for as
    // long as the process runs, even after whatever created this
    // SyncManager is done with it.
    private val scope = CoroutineScope(Dispatchers.IO)

    fun startBackgroundSync() {
        scope.launch {
            while (true) {
                api.pushPendingChanges()
                kotlinx.coroutines.delay(30_000)
            }
        }
    }
}
