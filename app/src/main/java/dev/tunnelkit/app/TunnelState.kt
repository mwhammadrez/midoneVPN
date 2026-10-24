package dev.tunnelkit.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Single source of truth shared between the service (writer) and the UI (reader).
 * Kept deliberately tiny: no DI, no repository layer — this is stage 1.
 */
object TunnelState {

    data class Stats(
        val inPackets: Long = 0,
        val outPackets: Long = 0,
        val bytes: Long = 0,
        val startedAt: Long = 0,
    )

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log.asStateFlow()

    private val _stats = MutableStateFlow(Stats())
    val stats: StateFlow<Stats> = _stats.asStateFlow()

    fun setRunning(value: Boolean) {
        _running.value = value
        _stats.value = if (value) Stats(startedAt = System.currentTimeMillis()) else Stats()
    }

    fun note(line: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        _log.update { current -> (listOf("$time  $line") + current).take(80) }
    }

    fun onInbound(bytes: Int) {
        _stats.update { it.copy(inPackets = it.inPackets + 1, bytes = it.bytes + bytes) }
    }

    fun onOutbound() {
        _stats.update { it.copy(outPackets = it.outPackets + 1) }
    }
}
