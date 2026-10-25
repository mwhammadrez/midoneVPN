package com.example.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// The caller now owns and injects the scope, and is responsible for
// cancelling it (e.g. an application-scoped CoroutineScope with a
// documented lifetime, or a scope tied to whatever object actually owns
// SyncManager's lifetime - not GlobalScope, not a scope SyncManager
// creates and never releases).
class SyncManager(
    private val api: SyncApi,
    private val scope: CoroutineScope,
) {
    private var syncJob: Job? = null

    fun startBackgroundSync() {
        syncJob?.cancel()
        syncJob = scope.launch {
            while (true) {
                api.pushPendingChanges()
                delay(30_000)
            }
        }
    }

    fun stopBackgroundSync() {
        syncJob?.cancel()
        syncJob = null
    }
}
