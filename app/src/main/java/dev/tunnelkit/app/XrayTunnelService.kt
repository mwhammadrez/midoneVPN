package dev.tunnelkit.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import dev.tunnelkit.core.diag.DiagLog
import androidx.core.app.NotificationCompat
import dev.tunnelkit.core.engine.EngineConfig
import dev.tunnelkit.core.engine.EngineCoordinator
import dev.tunnelkit.core.engine.ConfigFormat
import dev.tunnelkit.core.model.ConnectionState
import dev.tunnelkit.core.model.EngineId
import dev.tunnelkit.core.profile.ProfileStore
import dev.tunnelkit.engine.xray.LibXrayCore
import dev.tunnelkit.engine.xray.XrayAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real tunnel: this service owns the VpnService lifecycle, creates the TUN, and hands its file
 * descriptor to Xray through `XRAY_TUN_FD`. Xray then owns packet flow; there is no second pump.
 *
 * Teardown order matters and is fixed here:
 *   coordinator.stop()  ->  close the TUN fd  ->  stopForeground  ->  stopSelf
 */
class XrayTunnelService : VpnService() {

    companion object {
        private const val TAG = "MidONe"
        const val ACTION_START = "dev.tunnelkit.xray.START"
        const val ACTION_STOP = "dev.tunnelkit.xray.STOP"

        private const val CHANNEL_ID = "tunnel"
        private const val NOTIFICATION_ID = 11
        private const val TUN_ADDRESS = "198.18.0.1"
        private const val TUN_PREFIX = 15
        private const val MTU = 1400
        private const val PROBE_URL = "https://www.gstatic.com/generate_204"
        private const val PROBE_TIMEOUT_MS = 4_000

        private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
        val state: StateFlow<ConnectionState> = _state.asStateFlow()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val busy = AtomicBoolean(false)
    private var tun: ParcelFileDescriptor? = null

    private val coordinator by lazy {
        EngineCoordinator(
            onEvent = { Log.i(TAG, "coordinator: $it"); DiagLog.event("coordinator: $it") },
            engines = mapOf(
                EngineId("xray") to XrayAdapter(
                    core = LibXrayCore(),
                    protect = { fd -> protect(fd.toInt()) },
                    dnsServer = "1.1.1.1:53",
                    mtu = MTU,
                )
            ),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            shutdown()
            return START_NOT_STICKY
        }
        startAsForeground()
        connect()
        return START_STICKY
    }

    private fun connect() {
        if (!busy.compareAndSet(false, true)) return

        val profile = ProfileStore(this).selected()
        if (profile == null) {
            _state.value = ConnectionState.Failed(
                dev.tunnelkit.core.model.Failure.MissingBootstrap("no profile imported")
            )
            postState()
            shutdown()
            return
        }
        val link = profile.link

        val pfd = Builder()
            .setSession("TunnelKit")
            .setMtu(MTU)
            .setBlocking(true)
            .addAddress(TUN_ADDRESS, TUN_PREFIX)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)
            .addDnsServer("1.1.1.1")
            .addRoute("1.1.1.1", 32)
            .addDisallowedApplication(packageName)
            .establish()

        Log.i(TAG, "TUN establish -> ${if (pfd == null) "null" else "fd=${pfd.fd}"}"); DiagLog.event("establish=${if (pfd == null) "null" else "ok fd=${pfd.fd}"}")
        if (pfd == null) {
            _state.value = ConnectionState.Failed(
                dev.tunnelkit.core.model.Failure.PermissionDenied("establish() returned null")
            )
            postState()
            shutdown()
            return
        }
        tun = pfd

        scope.launch {
            coordinator.connect(
                EngineId("xray"),
                EngineConfig(
                    format = ConfigFormat.VLESS_LINK,
                    payload = link,
                    runtime = mapOf(EngineConfig.RUNTIME_TUN_FD to pfd.fd.toString()),
                ),
            )
            val result = coordinator.state.value
            Log.i(TAG, "connect result: $result"); DiagLog.event("state=$result")
            _state.value = result
            postState()

            if (result is ConnectionState.Connected) {
                val ok = probe()
                Log.i(TAG, "health probe: ${if (ok) "passed" else "failed"}"); DiagLog.event("probe=${if (ok) "passed" else "failed"}")
                if (!ok) {
                    // Interface is up but traffic does not flow: report it instead of pretending.
                    _state.value = ConnectionState.Failed(
                        dev.tunnelkit.core.model.Failure.Timeout("tunnel up but probe failed"),
                        EngineId("xray"),
                    )
                    postState()
                    teardownTunnel()
                }
            }
        }
    }

    /** The only proof of success: a real HTTP request that must travel through the tunnel. */
    private fun probe(): Boolean = runCatching {
        val conn = URL(PROBE_URL).openConnection() as HttpURLConnection
        conn.connectTimeout = PROBE_TIMEOUT_MS
        conn.readTimeout = PROBE_TIMEOUT_MS
        conn.instanceFollowRedirects = false
        conn.requestMethod = "GET"
        try {
            conn.responseCode in 200..399
        } finally {
            conn.disconnect()
        }
    }.getOrDefault(false)

    private fun teardownTunnel() {
        scope.launch {
            runCatching { coordinator.disconnect() }
            runCatching { tun?.close() }
            tun = null
            busy.set(false)
        }
    }

    private fun shutdown() {
        scope.launch {
            runCatching { coordinator.disconnect() }
            runCatching { tun?.close() }
            tun = null
            busy.set(false)
            _state.value = ConnectionState.Stopped
            postState()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onRevoke() {
        _state.value = ConnectionState.Failed(
            dev.tunnelkit.core.model.Failure.PermissionDenied("revoked by system")
        )
        postState()
        shutdown()
        super.onRevoke()
    }

    override fun onDestroy() {
        runCatching { tun?.close() }
        tun = null
        scope.cancel()
        super.onDestroy()
    }

    private fun postState() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(_state.value))
    }

    private fun startAsForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Tunnel", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification = buildNotification(_state.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(state: ConnectionState): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, XrayTunnelService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("TunnelKit — ${state::class.simpleName}")
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(0, "Disconnect", stop)
            .build()
    }
}
