package dev.tunnelkit.app

import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * The heart of the tunnel: a blocking read loop on the TUN file descriptor.
 *
 * Everything that "goes into the VPN" arrives here as a raw IP packet, and everything
 * this loop writes back is injected into the device's network stack as if it came from
 * the wire. In stage 1 we only answer pings; in stage 2 this is where the SOCKS/TCP
 * forwarding — or the WireGuard handshake — plugs in.
 */
class TunnelLoop(private val tun: ParcelFileDescriptor) : Thread("tun-loop") {

    @Volatile
    private var running = true

    private val input = FileInputStream(tun.fileDescriptor)
    private val output = FileOutputStream(tun.fileDescriptor)

    var packetsIn = 0L
        private set
    var repliesOut = 0L
        private set

    override fun run() {
        val buffer = ByteArray(MAX_PACKET)
        TunnelState.note("packet pump started (mtu ${TunnelVpnService.MTU})")

        while (running) {
            val read = try {
                input.read(buffer)
            } catch (e: IOException) {
                if (running) TunnelState.note("read failed: ${e.message}")
                break
            }
            if (read <= 0) continue

            packetsIn++
            TunnelState.onInbound(read)

            val header = Ipv4.Header(buffer)
            if (!header.isValid) continue

            if (packetsIn <= 5L || packetsIn % 25L == 0L) {
                TunnelState.note(
                    "${Ipv4.protocolName(header.protocol)} ${header.source} -> " +
                        "${header.destination} (${header.totalLength} B)"
                )
            }

            val reply = Ipv4.echoReply(buffer, read) ?: continue
            try {
                output.write(reply)
                repliesOut++
                TunnelState.onOutbound()
                TunnelState.note("echo reply -> ${header.source}")
            } catch (e: IOException) {
                if (running) TunnelState.note("write failed: ${e.message}")
                break
            }
        }

        TunnelState.note("packet pump stopped")
    }

    /** Closing the fd is what actually unblocks a pending read(). */
    fun shutdown() {
        running = false
        try {
            tun.close()
        } catch (_: IOException) {
        }
    }

    companion object {
        private const val MAX_PACKET = 32 * 1024
    }
}
