package dev.tunnelkit.core.config

/**
 * Composes a full Xray client config around an outbound object.
 *
 * Two shapes are supported:
 *  - [socksConfig]  — Xray listens on a local SOCKS port; a tun2socks pump feeds it.
 *  - [tunConfig]    — Xray consumes the Android VpnService fd directly through `xray.tun.fd`.
 *                     Verified on 2026-10-24 against Xray 26.10.21: with `XRAY_TUN_FD=999` the
 *                     binary fails with "bad file descriptor" (it read the variable); without it,
 *                     "operation not permitted" (it tried to create its own TUN).
 */
object XrayConfigBuilder {

    /**
     * Private ranges are listed explicitly.
     *
     * `geoip:private` requires geoip.dat to sit next to the binary. libXray ships no data files, so
     * Xray aborts at config load with "failed to open geoip.dat" and never opens a socket — the
     * failure looks like a dead server. Plain CIDRs need no external asset.
     */
    private val PRIVATE_CIDRS = listOf(
        "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16", "127.0.0.0/8",
        "169.254.0.0/16", "::1/128", "fc00::/7", "fe80::/10",
    )

    private val DIRECT_ROUTING =
        """{"domainStrategy":"AsIs","rules":[
          {"type":"field","ip":[""" + PRIVATE_CIDRS.joinToString(",") { "\"$it\"" } + """],"outboundTag":"direct"},
          {"type":"field","network":"udp","port":53,"outboundTag":"proxy"},
          {"type":"field","network":"tcp,udp","outboundTag":"proxy"}
        ]}"""

    fun socksConfig(outboundJson: String, socksPort: Int = 10808): String = buildString {
        append("""{"log":{"loglevel":"warning"},"inbounds":[""")
        append("""{"tag":"socks-in","listen":"127.0.0.1","port":""").append(socksPort)
        append(""","protocol":"socks","settings":{"udp":true,"auth":"noauth"}}],""")
        append(""""outbounds":[""").append(outboundJson)
        append(""",{"tag":"direct","protocol":"freedom"}],"routing":""")
        append(DIRECT_ROUTING).append("}")
    }

    /**
     * @param fd the file descriptor from `VpnService.Builder.establish()`.
     * @param mtu must match what the service asked for, otherwise large packets stall.
     */
    fun tunConfig(
        outboundJson: String,
        fd: Int,
        mtu: Int = 1400,
        dns: String? = "1.1.1.1",
        /** Interface name of the device behind [fd]. Xray rejects the fd if it does not match. */
        tunName: String = "tun0",
    ): String = buildString {
        append("""{"log":{"loglevel":"warning"},""")
        // libXray removed SetTunFd: the fd must travel as `xray.tun.fd` in the root `env` object,
        // which Xray applies with os.Setenv before building the tun inbound. The old key
        // `XRAY_TUN_FD` is not read by the core (verified against libXray's README and the
        // 26.10.21 binary), so the inbound silently failed to attach and no packet ever moved.
        append(""""env":{"xray.tun.fd":""").append(quote(fd.toString())).append("},")
        append(""""inbounds":[{"tag":"tun-in","protocol":"tun","settings":{""")
        append(""""name":""").append(quote(tunName)).append(""","mtu":""").append(mtu)
        if (dns != null) append(""","dns":[""").append(quote(dns)).append("]")
        append("}")   // close "settings"; the inbound itself closes after "sniffing"
        // Sniffing recovers the requested hostname from TLS/HTTP so routing and DNS work even when
        // the client resolved a poisoned address.
        append(""","sniffing":{"enabled":true,"destOverride":["http","tls","quic"],"routeOnly":false}}],""")
        append(""""outbounds":[""").append(outboundJson)
        append(""",{"tag":"direct","protocol":"freedom"}],"routing":""")
        append(DIRECT_ROUTING).append("}")
    }

    private fun quote(s: String): String = buildString {
        append('"')
        for (c in s) when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            else -> append(c)
        }
        append('"')
    }
}
