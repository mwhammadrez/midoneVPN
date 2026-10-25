package com.example.export

import kotlinx.coroutines.withContext
import java.io.File

class ReportExporter(private val dispatchers: DispatcherProvider) {

    suspend fun exportToFile(report: String, target: File) {
        withContext(dispatchers.io) {
            target.writeText(report)
        }
    }
}

// DispatcherProvider is injected (see kotlin-coroutines-flow) rather than
// hardcoding Dispatchers.IO, so this class is testable with a
// TestDispatcher without touching real threads.
interface DispatcherProvider {
    val io: kotlinx.coroutines.CoroutineDispatcher
    val default: kotlinx.coroutines.CoroutineDispatcher
    val main: kotlinx.coroutines.CoroutineDispatcher
}
