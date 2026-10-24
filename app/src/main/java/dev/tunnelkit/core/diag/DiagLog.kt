package dev.tunnelkit.core.diag

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bounded, redacted, in-memory diagnostics.
 *
 * Every line is sanitised on the way in: links, UUIDs, keys and passwords never reach the buffer,
 * because the buffer is copied into chat windows by users.
 */
object DiagLog {

    private const val MAX_LINES = 200

    private val time = SimpleDateFormat("HH:mm:ss", Locale.US)

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    fun header(deviceSummary: String) {
        add("MidONe $deviceSummary")
        add("---- session start ----")
    }

    fun event(message: String) = add(message)

    fun clear() {
        _lines.value = emptyList()
    }

    fun dump(): String = _lines.value.joinToString("\n")

    private fun add(raw: String) {
        val line = "${time.format(Date())}  ${redact(raw)}"
        _lines.update { current ->
            val next = current + line
            if (next.size > MAX_LINES) next.takeLast(MAX_LINES) else next
        }
    }

    /** Removes anything that must never leave the device. */
    fun redact(input: String): String {
        var s = input
        s = s.replace(Regex("""vless://[^\s]+"""), "vless://<redacted>")
        s = s.replace(Regex("""\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\b"""), "<uuid>")
        s = s.replace(Regex("""(?i)(privatekey|presharedkey|password|pbk|sid)\s*=\s*\S+""")) { m ->
            m.groupValues[1] + "=<redacted>"
        }
        s = s.replace(Regex("""\b[A-Za-z0-9+/]{43}=\b"""), "<key>")
        return s
    }
}
