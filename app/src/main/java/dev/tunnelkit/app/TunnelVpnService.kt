package dev.tunnelkit.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat

/**
 * A VPN app on Android is a Service that:
 *   1. asks the system for permission (VpnService.prepare, done from the Activity),
 *   2. builds a TUN interface with an address and a route,
 *   3. gets a file descriptor for that interface,
 *   4. reads/writes raw IP packets on it forever,
 *   5. stays alive as a foreground service.
 *
 * There is no "magic" beyond that. The encryption, if any, happens at step 4 —
 * which is exactly what stage 2 will add.
 */
class TunnelVpnService : VpnService() {

    companion object {
        const val ACTION_START = "dev.tunnelkit.app.action.START"
        const val ACTION_STOP = "dev.tunnelkit.app.action.STOP"

        const val TUN_ADDRESS = "10.111.0.2"
        const val TUN_GATEWAY = "10.111.0.1"
        const val TUN_PREFIX = 24
        const val MTU = 1500

        private const val CHANNEL_ID = "tunnel"
        private const val NOTIFICATION_ID = 7
    }

    private var tun: ParcelFileDescriptor? = null
    private var loop: TunnelLoop? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                teardown()
                stopSelf()
                return START_NOT_STICKY
            }

            else -> startTunnel()
        }
        return START_STICKY
    }

    private fun startTunnel() {
        if (tun != null) return

        createChannel()
        startAsForeground(buildNotification())

        try {
            val descriptor = Builder()
                .setSession("TunnelKit")
                .setMtu(MTU)
                .addAddress(TUN_ADDRESS, TUN_PREFIX)
                // Stage 1: only the tunnel subnet is routed, so the phone keeps its
                // normal internet. Stage 2 flips this to 0.0.0.0/0 (full tunnel).
                .addRoute(TUN_GATEWAY, TUN_PREFIX)
                .setBlocking(true)
                .establish()
                ?: throw IllegalStateException("establish() returned null")

            tun = descriptor
            TunnelState.setRunning(true)
            TunnelState.note("tun up: $TUN_ADDRESS/$TUN_PREFIX via $TUN_GATEWAY")

            loop = TunnelLoop(descriptor).also { it.start() }
        } catch (e: Exception) {
            TunnelState.note("could not start tunnel: ${e.message}")
            teardown()
            stopSelf()
        }
    }

    private fun teardown() {
        loop?.shutdown()
        loop = null
        tun = null
        TunnelState.setRunning(false)
        TunnelState.note("tunnel down")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    /** The user (or the system) revoked our VPN permission: clean up immediately. */
    override fun onRevoke() {
        TunnelState.note("vpn permission revoked by system")
        teardown()
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    private fun startAsForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14 requires the type to match the manifest declaration.
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Tunnel", NotificationManager.IMPORTANCE_LOW).apply {
                description = "VPN connection state"
                setShowBadge(false)
            }
        )
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, TunnelVpnService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("Tunnel is up")
            .setContentText("$TUN_ADDRESS · packets are flowing")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(open)
            .addAction(0, "Disconnect", stop)
            .build()
    }
}
