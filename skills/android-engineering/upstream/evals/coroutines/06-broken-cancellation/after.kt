package com.example.upload

import kotlinx.coroutines.CancellationException

class UploadWorker(private val api: UploadApi) {

    suspend fun uploadAll(files: List<ByteArray>) {
        for (file in files) {
            try {
                api.upload(file)
            } catch (e: CancellationException) {
                throw e // never swallow this - let cancellation propagate
            } catch (e: Exception) {
                log("upload failed: ${e.message}")
            }
        }
    }

    private fun log(message: String) { /* ... */ }
}
