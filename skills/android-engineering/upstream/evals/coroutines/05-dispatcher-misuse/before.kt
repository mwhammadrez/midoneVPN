package com.example.export

import java.io.File

class ReportExporter {

    // BUG: called from ViewModel on Dispatchers.Main.immediate
    // (viewModelScope's default), but this function does blocking disk
    // I/O with no dispatcher switch - it blocks the main thread for the
    // duration of the write, freezing the UI for large reports.
    suspend fun exportToFile(report: String, target: File) {
        target.writeText(report) // blocking I/O, no withContext
    }
}
