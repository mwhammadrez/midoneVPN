package dev.tunnelkit.core.config

/**
 * Renders a parsed `wg-quick` config as an Xray `wireguard` outbound.
 *
 * Doing it inside Xray means no second core, no NDK build and no root: the tun inbound owns the
 * device and the outbound speaks WireGuard in userspace.
 */
object WireGuardOutbound {

    fun toOutboundJson(config: WireGuardConfig.Config): String = buildString {
        val iface = config.interfaceSection
        append("""{"tag":"proxy","protocol":"wireguard","settings":{""")
        append("\"secretKey\":").append(quote(iface.privateKey?.reveal() ?: ""))
        val addresses = iface.addresses.map { stripPrefix(it) }.distinct()
        append(",\"address\":[").append(addresses.joinToString(",") { quote(it) }).append("]")
        if (iface.mtu != null) append(",\"mtu\":").append(iface.mtu)
        append(",\"peers\":[")
        append(config.peers.joinToString(",") { peer ->
            buildString {
                append("{\"publicKey\":").append(quote(peer.publicKey))
                peer.presharedKey?.let { append(",\"preSharedKey\":").append(quote(it.reveal())) }
                peer.endpoint?.let { append(",\"endpoint\":").append(quote(it)) }
                append(",\"allowedIPs\":[")
                append(peer.allowedIps.joinToString(",") { quote(it) })
                append("]")
                if (peer.persistentKeepalive != null) {
                    append(",\"keepAlive\":").append(peer.persistentKeepalive)
                }
                append("}")
            }
        })
        append("]}}")
    }

    /** Xray wants bare addresses in `address`; the prefix only matters for `allowedIPs`. */
    private fun stripPrefix(cidr: String): String = cidr.substringBefore('/')

    private fun quote(s: String): String = buildString {
        append('"')
        for (c in s) when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            else -> append(c)
        }
        append('"')
    }
}
