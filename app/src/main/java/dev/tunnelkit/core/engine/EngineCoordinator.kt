package dev.tunnelkit.core.engine

import dev.tunnelkit.core.model.ConnectionState
import dev.tunnelkit.core.model.EngineId
import dev.tunnelkit.core.model.Failure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The one place that owns "the connection".
 *
 * Invariants (tested in EngineCoordinatorTest):
 *  - at most one engine is active, because Android allows one VpnService per user profile;
 *  - switching engines always stops the previous one before starting the next;
 *  - a failure after start() attempted always triggers cleanup;
 *  - connect() while connected to the same engine is a no-op (idempotent restart);
 *  - an engine that throws [EngineBlockedException] yields [ConnectionState.Blocked], not Failed.
 */
class EngineCoordinator(
    private val engines: Map<EngineId, VpnEngine>,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val onEvent: (String) -> Unit = {},
) {

    private val mutex = Mutex()
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private var active: VpnEngine? = null
    private var activeId: EngineId? = null

    suspend fun connect(id: EngineId, config: EngineConfig) = mutex.withLock {
        if (activeId == id && _state.value is ConnectionState.Connected) return@withLock

        // Never run two owners at once.
        stopLocked()

        val engine = engines[id]
        if (engine == null) {
            _state.value = ConnectionState.Failed(
                Failure.UnsupportedFeature("no engine registered as ${id.value}"), id,
            )
            return@withLock
        }

        _state.value = ConnectionState.Validating
        try {
            val check = engine.validate(config)
            if (!check.ok) {
                _state.value = ConnectionState.Failed(Failure.InvalidConfig(), id)
                onEvent("validate failed: ${check.reason}")
                return@withLock
            }

            _state.value = ConnectionState.Preparing(id)
            engine.prepare()

            _state.value = ConnectionState.Connecting(id)
            engine.start(config)

            active = engine
            activeId = id
            _state.value = ConnectionState.Connected(id, clock())
            onEvent("connected: ${engine.descriptor.engineName}")
        } catch (c: CancellationException) {
            runCatching { engine.stop() }   // start() may have half-acquired resources
            _state.value = ConnectionState.Stopped
            throw c
        } catch (blocked: EngineBlockedException) {
            runCatching { engine.stop() }
            _state.value = ConnectionState.Blocked(Failure.LicenseBlocked, id)
            onEvent("blocked: ${blocked.message}")
        } catch (t: Throwable) {
            runCatching { engine.stop() }
            _state.value = ConnectionState.Failed(mapFailure(t), id)
            onEvent("failed: ${t.javaClass.simpleName}")
        }
    }

    suspend fun disconnect() = mutex.withLock {
        if (active == null) {
            _state.value = ConnectionState.Stopped
            return@withLock
        }
        _state.value = ConnectionState.Stopping
        stopLocked()
        _state.value = ConnectionState.Stopped
    }

    /** Stop-then-start, always in that order. */
    suspend fun switchTo(id: EngineId, config: EngineConfig) {
        disconnect()
        connect(id, config)
    }

    fun activeEngineId(): EngineId? = activeId

    private suspend fun stopLocked() {
        val engine = active ?: return
        _state.value = ConnectionState.Stopping
        runCatching { engine.stop() }
            .onFailure { onEvent("stop failed: ${it.javaClass.simpleName}") }
        active = null
        activeId = null
    }

    private fun mapFailure(t: Throwable): Failure = when (t) {
        is EngineFailureException -> t.failure
        is IllegalArgumentException -> Failure.InvalidConfig()
        is SecurityException -> Failure.PermissionDenied(t.message ?: "")
        is java.net.UnknownHostException -> Failure.DnsFailure(t.message ?: "")
        is java.net.SocketTimeoutException -> Failure.Timeout(t.message ?: "")
        is java.net.ConnectException -> Failure.EndpointUnreachable(t.message ?: "")
        is javax.net.ssl.SSLException -> Failure.TlsFailure(t.message ?: "")
        is UnsatisfiedLinkError -> Failure.NativeCrash(t.message ?: "")
        else -> Failure.Unknown(t.javaClass.simpleName)
    }
}
