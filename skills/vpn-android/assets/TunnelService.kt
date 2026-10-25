package dev.tunnelkit.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Production-shaped VpnService skeleton.
 *
 * Deliberately shows the four things that are always missed:
 *   1. system/always-on starts arrive WITHOUT our extra -> persist config, don't trust intent
 *   2. pump runs on its own thread; the service never blocks
 *   3. network handover triggers a transport restart, not a silent dead tunnel
 *   4. a watchdog notices "connected but frozen" and restarts the core
 *
 * Wire [pump] to HevBridge (tun2socks), to libXray's runXray (Recipe B) or to GoBackend (Recipe C).
 */
class TunnelService : VpnService() {

    companion object {
        const val ACTION_START = "dev.tunnelkit.action.START"
        const val ACTION_STOP = "dev.tunnelkit.action.STOP"
        const val EXTRA_USER_STARTED = "user_started"

        private const val CHANNEL_ID = "tunnel"
        private const val NOTIFICATION_ID = 7
        private const val TUN_ADDRESS = "198.18.0.1"
        private const val TUN_PREFIX = 15          // benchmarking range: never collides with LAN
        private const val MTU = 1400
    }

    private var tun: ParcelFileDescriptor? = null
    private var pump: Thread? = null
    private var watchdog: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val started = AtomicBoolean(false)

    @Volatile private var lastTx = -1L
    @Volatile private var staleTicks = 0

    private val connectivity by lazy { getSystemService(ConnectivityManager::class.java) }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = onTransportChanged()
        override fun onLost(network: Network) = onTransportChanged()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val userStarted = intent?.getBooleanExtra(EXTRA_USER_STARTED, false) ?: false

        if (action == ACTION_STOP) {
            teardown()
            stopSelf()
            return START_NOT_STICKY
        }

        // No action + no extra => the system (always-on VPN) started us.
        if (action == null && !userStarted) {
            // Load the persisted profile here. Never show "Disconnect" UI in this mode.
        }

        startForegroundNow()
        if (started.compareAndSet(false, true)) startTunnel()
        return START_STICKY
    }

    private fun startTunnel() {
        val pfd = Builder()
            .setSession("TunnelKit")
            .setMtu(MTU)
            .setBlocking(true)
            .addAddress(TUN_ADDRESS, TUN_PREFIX)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)                  // IPv6 also inside the tunnel, or it leaks
            .addDnsServer("1.1.1.1")            // in-tunnel resolver
            .addRoute("1.1.1.1", 32)
            .addDisallowedApplication(packageName)   // don't tunnel ourselves
            .establish()

        if (pfd == null) {                      // not prepared / revoked / another VPN won
            teardown()
            stopSelf()
            return
        }
        tun = pfd

        startPump(pfd)
        startWatchdog()
        registerNetworkCallback()
    }

    /** Replace with your core. Must block until stopped. */
    private fun startPump(pfd: ParcelFileDescriptor) {
        pump = Thread({
            HevBridge.init(pfd.fd)
            HevBridge.run()
            HevBridge.fini()
        }, "tunnel-pump").apply { start() }
    }

    private fun startWatchdog() {
        watchdog?.cancel()
        watchdog = scope.launch {
            while (started.get()) {
                delay(5_000)
                val tx = HevBridge.stats().firstOrNull() ?: -1
                if (tx >= 0 && tx == lastTx) {
                    if (++staleTicks >= 6) {          // 30 s with zero packets
                        onTransportChanged()
                        staleTicks = 0
                    }
                } else {
                    staleTicks = 0
                }
                lastTx = tx
            }
        }
    }

    /**
     * Called on Wi-Fi <-> cellular handover and by the watchdog.
     * The TUN stays valid; the *upstream* connection is what died. Restart the core,
     * not the interface.
     */
    private fun onTransportChanged() {
        if (!started.get()) return
        pump?.let { thread ->
            HevBridge.stop()
            runCatching { thread.join(2_000) }
        }
        tun?.let { startPump(it) }
    }

    private fun registerNetworkCallback() {
        runCatching { connectivity.registerDefaultNetworkCallback(networkCallback) }
    }

    private fun teardown() {
        if (started.compareAndSet(true, false).not()) return
        runCatching { connectivity.unregisterNetworkCallback(networkCallback) }
        watchdog?.cancel()
        runCatching { HevBridge.stop() }
        runCatching { pump?.join(2_000) }
        pump = null
        runCatching { tun?.close() }
        tun = null
        scope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onRevoke() {                 // may arrive off the main thread
        teardown()
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    private fun startForegroundNow() {
        createChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Tunnel", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, Class.forName("$packageName.MainActivity")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, TunnelService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)   // replace with your own ic_stat
            .setContentTitle("Tunnel is up")
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(0, "Disconnect", stop)
            .build()
    }

    /** Start helper that marks the intent as user-initiated. */
    fun start(context: Context) {
        val intent = Intent(context, TunnelService::class.java)
            .setAction(ACTION_START)
            .putExtra(EXTRA_USER_STARTED, true)
        context.startForegroundService(intent)
    }
}
