package dev.tunnelkit.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Host-side checks for the packet code. No device needed — this is the part of a VPN
 * app you can and should test on the JVM.
 */
class Ipv4Test {

    private fun buildPacket(
        protocol: Int,
        src: ByteArray,
        dst: ByteArray,
        payload: ByteArray,
    ): ByteArray {
        val total = 20 + payload.size
        val p = ByteArray(total)
        p[0] = 0x45                       // IPv4, 5 words
        p[2] = ((total shr 8) and 0xFF).toByte()
        p[3] = (total and 0xFF).toByte()
        p[8] = 64                         // ttl
        p[9] = protocol.toByte()
        src.copyInto(p, 12)
        dst.copyInto(p, 16)
        payload.copyInto(p, 20)
        val sum = Ipv4.checksum(p, 0, 20)
        p[10] = ((sum shr 8) and 0xFF).toByte()
        p[11] = (sum and 0xFF).toByte()
        return p
    }

    private fun echoRequest(id: Int, seq: Int, body: String): ByteArray {
        val data = body.toByteArray()
        val icmp = ByteArray(8 + data.size)
        icmp[0] = 8                             // echo request
        icmp[4] = ((id shr 8) and 0xFF).toByte()
        icmp[5] = (id and 0xFF).toByte()
        icmp[6] = ((seq shr 8) and 0xFF).toByte()
        icmp[7] = (seq and 0xFF).toByte()
        data.copyInto(icmp, 8)
        val sum = Ipv4.checksum(icmp, 0, icmp.size)
        icmp[2] = ((sum shr 8) and 0xFF).toByte()
        icmp[3] = (sum and 0xFF).toByte()
        return icmp
    }

    @Test
    fun echoRequestBecomesValidEchoReply() {
        val client = byteArrayOf(10, 111, 0, 2)
        val gateway = byteArrayOf(10, 111, 0, 1)
        val request = buildPacket(1, client, gateway, echoRequest(0x1234, 7, "hello-tunnel"))

        val reply = Ipv4.echoReply(request, request.size)
        assertNotNull(reply)
        reply!!

        val header = Ipv4.Header(reply)
        assertEquals(4, header.version)
        assertEquals(40, header.totalLength)  // 20 IP + 8 ICMP + 12 payload
        assertEquals("10.111.0.1", header.source)
        assertEquals("10.111.0.2", header.destination)

        // type 0 = echo reply, code 0
        val icmp = header.payloadOffset
        assertEquals(0, reply[icmp].toInt())
        assertEquals(0, reply[icmp + 1].toInt())

        // A packet whose checksum is correct verifies to zero.
        assertEquals(0, Ipv4.checksum(reply, 0, header.headerLength))
        assertEquals(0, Ipv4.checksum(reply, icmp, header.payloadLength))

        // Body must be echoed back untouched.
        assertEquals("hello-tunnel", String(reply, icmp + 8, header.payloadLength - 8))
    }

    @Test
    fun tcpPacketIsNotAnswered() {
        val p = buildPacket(6, byteArrayOf(10, 111, 0, 2), byteArrayOf(1, 1, 1, 1), ByteArray(20))
        assertNull(Ipv4.echoReply(p, p.size))
    }

    @Test
    fun malformedPacketIsRejected() {
        assertNull(Ipv4.echoReply(ByteArray(10), 10))
        val garbage = ByteArray(40)
        assertNull(Ipv4.echoReply(garbage, 40))
    }

    @Test
    fun headerParsingMatchesTheBytes() {
        val p = buildPacket(17, byteArrayOf(10, 111, 0, 2), byteArrayOf(8, 8, 8, 8), ByteArray(12))
        val h = Ipv4.Header(p)
        assertEquals("UDP", Ipv4.protocolName(h.protocol))
        assertEquals("10.111.0.2", h.source)
        assertEquals("8.8.8.8", h.destination)
        assertEquals(20, h.headerLength)
        assertEquals(12, h.payloadLength)
    }
}
