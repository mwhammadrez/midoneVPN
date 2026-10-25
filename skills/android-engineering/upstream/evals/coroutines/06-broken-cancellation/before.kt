package com.example.upload

import kotlinx.coroutines.CancellationException

class UploadWorker(private val api: UploadApi) {

    suspend fun uploadAll(files: List<ByteArray>) {
        for (file in files) {
            try {
                api.upload(file)
            } catch (e: Exception) {
                // BUG: catches CancellationException along with every
                // other exception and swallows it instead of rethrowing.
                // If the enclosing coroutine is cancelled mid-loop (e.g.
                // the user navigates away), this loop keeps going,
                // uploading the remaining files anyway, and the
                // cancellation never propagates to whatever awaited this
                // coroutine.
                log("upload failed: ${e.message}")
            }
        }
    }

    private fun log(message: String) { /* ... */ }
}
