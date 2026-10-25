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
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import dev.tunnelkit.core.diag.DiagLog
import dev.tunnelkit.core.net.Preflight
import dev.tunnelkit.core.model.Failure
import dev.tunnelkit.core.config.VlessLink
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
 * descriptor to Xray through `xray.tun.fd`. Xray then owns packet flow; there is no second pump.
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
        /** Bare IP: no DNS involved, so a failure here means packets are not flowing. */
        private const val IP_PROBE_URL = "https://1.1.1.1/cdn-cgi/trace"
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
        val parsedLink = VlessLink.parse(link).getOrElse { e ->
            _state.value = ConnectionState.Failed(Failure.InvalidConfig(e.message ?: "unparsable link"), EngineId("xray"))
            postState(); shutdown(); return
        }
        if (!preflight(parsedLink)) return

        val before = tunNames()
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

        val tunName = (tunNames() - before).firstOrNull() ?: "tun0"
        DiagLog.event("tun interface: $tunName")
        Log.i(TAG, "TUN establish -> ${if (pfd == null) "null" else "fd=${pfd.fd}"} name=$tunName"); DiagLog.event("establish=${if (pfd == null) "null" else "ok fd=${pfd.fd}"}")
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
                    runtime = mapOf(
                        EngineConfig.RUNTIME_TUN_FD to pfd.fd.toString(),
                        EngineConfig.RUNTIME_TUN_NAME to tunName,
                    ),
                ),
            )
            val result = coordinator.state.value
            Log.i(TAG, "connect result: $result"); DiagLog.event("state=$result")
            _state.value = result
            postState()

            if (result is ConnectionState.Connected) {
                val probe = probeThroughTunnel()
                DiagLog.event(probe.describe())
                Log.i(TAG, probe.describe())
                if (probe.failure != null) {
                    // Interface is up but traffic does not flow: report it instead of pretending.
                    _state.value = ConnectionState.Failed(probe.failure, EngineId("xray"))
                    postState()
                    teardownTunnel()
                }
            }
        }
    }

    private data class Probe(
        val tcpIp: Boolean,
        val dns: Boolean,
        val https: Boolean,
        val failure: dev.tunnelkit.core.model.Failure?,
    ) {
        fun describe(): String = "probe tcpIp=$tcpIp dns=$dns https=$https" +
            (failure?.let { " -> ${it.code}" } ?: "")
    }

    /**
     * Three steps, all bound to the VPN network, because this app excludes itself from the tunnel
     * (its own sockets must not loop back into the TUN). A plain HttpURLConnection from this process
     * therefore proves nothing: it bypasses the VPN.
     *
     *  tcpIp  bare IP over the tunnel -> is any packet flowing at all?
     *  dns    name lookup through the VPN network -> does UDP/53 survive the tunnel?
     *  https  a real HTTPS request -> end to end
     */
    private fun probeThroughTunnel(): Probe {
        val net = vpnNetwork()
        if (net == null) {
            return Probe(false, false, false, dev.tunnelkit.core.model.Failure.Unknown("no VPN network"))
        }

        val tcpIp = fetch(net, IP_PROBE_URL) != null

        val dns = runCatching {
            net.getAllByName("www.gstatic.com").isNotEmpty()
        }.getOrDefault(false)

        val https = fetch(net, PROBE_URL) != null

        val failure = when {
            !tcpIp -> dev.tunnelkit.core.model.Failure.Timeout("no packet flow through the tun (bare IP failed)")
            !dns -> dev.tunnelkit.core.model.Failure.DnsFailure("DNS through the tunnel failed")
            !https -> dev.tunnelkit.core.model.Failure.TlsFailure("HTTPS through the tunnel failed")
            else -> null
        }
        return Probe(tcpIp, dns, https, failure)
    }

    private fun vpnNetwork(): Network? = runCatching {
        val cm = getSystemService(ConnectivityManager::class.java)
        cm.allNetworks.firstOrNull { n ->
            cm.getNetworkCapabilities(n)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        }
    }.getOrNull()

    private fun fetch(net: Network, url: String): Int? = runCatching {
        val conn = net.openConnection(URL(url)) as HttpURLConnection
        conn.connectTimeout = PROBE_TIMEOUT_MS
        conn.readTimeout = PROBE_TIMEOUT_MS
        conn.instanceFollowRedirects = false
        conn.requestMethod = "GET"
        try {
            val code = conn.responseCode
            conn.inputStream.close()
            code
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    /**
     * Checks TCP -> TLS -> one HTTP request before the engine starts.
     *
     * Xray reports success as soon as its config loads, so without this a dead or misconfigured
     * endpoint still shows "Connected".
     */
    private fun preflight(link: VlessLink): Boolean {
        val result = Preflight.check(link)
        DiagLog.event(result.describe())
        Log.i(TAG, result.describe())
        val failure = result.failure
        val certPinned = !link.pinnedCertSha256.isNullOrBlank() || !link.verifyPeerCertByName.isNullOrBlank()
        val fatal = failure is Failure.EndpointUnreachable ||
            (failure is Failure.TlsFailure && !certPinned)
        if (fatal) {
            _state.value = ConnectionState.Failed(failure!!, EngineId("xray"))
            postState()
            shutdown()
            return false
        }
        return true
    }

    /** Interface names currently present; diffing before/after establish() reveals the TUN. */
    internal fun tunNames(): Set<String> = runCatching {
        java.net.NetworkInterface.getNetworkInterfaces()
            .toList()
            .filter { it.name.startsWith("tun") }
            .map { it.name }
            .toSet()
    }.getOrDefault(emptySet())

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
