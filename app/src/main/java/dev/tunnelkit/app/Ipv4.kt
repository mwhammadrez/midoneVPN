package dev.tunnelkit.app

/**
 * Just enough IPv4 to read what came out of the tunnel and answer it.
 *
 * A packet inside a TUN interface is a raw IP packet: 20-byte header, then payload.
 * Bit 0-3 of byte 0 is the version (must be 4), bits 4-7 are the header length in
 * 32-bit words. Byte 9 is the protocol. That is all we need for stage 1.
 */
object Ipv4 {

    const val PROTO_ICMP = 1
    const val PROTO_TCP = 6
    const val PROTO_UDP = 17

    class Header(private val b: ByteArray) {
        val version: Int = (b[0].toInt() shr 4) and 0x0F
        val headerLength: Int = (b[0].toInt() and 0x0F) * 4
        val totalLength: Int = ((b[2].toInt() and 0xFF) shl 8) or (b[3].toInt() and 0xFF)
        val protocol: Int = b[9].toInt() and 0xFF
        val source: String = ip(b, 12)
        val destination: String = ip(b, 16)
        val payloadOffset: Int get() = headerLength
        val payloadLength: Int get() = (totalLength - headerLength).coerceAtLeast(0)
        val isValid: Boolean
            get() = version == 4 &&
                headerLength in 20..60 &&
                totalLength >= headerLength &&
                totalLength <= b.size
    }

    fun ip(b: ByteArray, offset: Int): String = buildString {
        for (i in 0 until 4) {
            if (i > 0) append('.')
            append(b[offset + i].toInt() and 0xFF)
        }
    }

    fun protocolName(protocol: Int): String = when (protocol) {
        PROTO_ICMP -> "ICMP"
        PROTO_TCP -> "TCP"
        PROTO_UDP -> "UDP"
        else -> "IP/$protocol"
    }

    /**
     * If [packet] is an ICMP echo request, return a properly checksummed echo reply.
     * This is the smallest possible proof that the tunnel is real: the device pings
     * the gateway, the packet goes into the TUN fd, we answer, the kernel accepts it.
     */
    fun echoReply(packet: ByteArray, length: Int): ByteArray? {
        if (length < 20) return null
        val header = Header(packet)
        if (!header.isValid || header.protocol != PROTO_ICMP || header.payloadLength < 8) return null

        val icmp = header.payloadOffset
        val type = packet[icmp].toInt() and 0xFF
        if (type != 8) return null // 8 = echo request

        val out = packet.copyOf(header.totalLength)

        // Swap source and destination.
        for (i in 0 until 4) {
            val tmp = out[12 + i]
            out[12 + i] = out[16 + i]
            out[16 + i] = tmp
        }

        // 0 = echo reply, code 0.
        out[icmp] = 0
        out[icmp + 1] = 0

        // ICMP checksum covers the whole ICMP message.
        out[icmp + 2] = 0
        out[icmp + 3] = 0
        val icmpSum = checksum(out, icmp, header.payloadLength)
        out[icmp + 2] = ((icmpSum shr 8) and 0xFF).toByte()
        out[icmp + 3] = (icmpSum and 0xFF).toByte()

        // IPv4 header checksum covers only the header.
        out[10] = 0
        out[11] = 0
        val ipSum = checksum(out, 0, header.headerLength)
        out[10] = ((ipSum shr 8) and 0xFF).toByte()
        out[11] = (ipSum and 0xFF).toByte()

        return out
    }

    /** Standard 16-bit one's-complement checksum (RFC 1071). */
    fun checksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = offset
        val end = offset + length
        while (i + 1 < end) {
            sum += ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            i += 2
        }
        if (i < end) sum += (data[i].toInt() and 0xFF) shl 8
        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        return sum.inv() and 0xFFFF
    }
}
