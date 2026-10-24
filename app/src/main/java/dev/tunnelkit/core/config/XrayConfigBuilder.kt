package dev.tunnelkit.core.config

/**
 * Composes a full Xray client config around an outbound object.
 *
 * Two shapes are supported:
 *  - [socksConfig]  — Xray listens on a local SOCKS port; a tun2socks pump feeds it.
 *  - [tunConfig]    — Xray consumes the Android VpnService fd directly through `XRAY_TUN_FD`.
 *                     Verified on 2026-10-24 against Xray 26.10.21: with `XRAY_TUN_FD=999` the
 *                     binary fails with "bad file descriptor" (it read the variable); without it,
 *                     "operation not permitted" (it tried to create its own TUN).
 */
object XrayConfigBuilder {

    private const val DIRECT_ROUTING =
        ""","routing":{"domainStrategy":"AsIs","rules":[
          {"type":"field","ip":["geoip:private"],"outboundTag":"direct"},
          {"type":"field","network":"udp","port":53,"outboundTag":"proxy"}
        ]}"""

    fun socksConfig(outboundJson: String, socksPort: Int = 10808): String = buildString {
        append("""{"log":{"loglevel":"warning"},"inbounds":[""")
        append("""{"tag":"socks-in","listen":"127.0.0.1","port":""").append(socksPort)
        append(""","protocol":"socks","settings":{"udp":true,"auth":"noauth"}}],""")
        append(""""outbounds":[{"tag":"direct","protocol":"freedom"},""")
        append(outboundJson).append("]")
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
    ): String = buildString {
        append("""{"log":{"loglevel":"warning"},""")
        // Xray-core applies root `env` with os.Setenv before building the tun inbound.
        append(""""env":{"XRAY_TUN_FD":""").append(quote(fd.toString())).append("},")
        append(""""inbounds":[{"tag":"tun-in","protocol":"tun","settings":{""")
        append(""""name":"tun0","mtu":""").append(mtu)
        if (dns != null) append(""","dns":[""").append(quote(dns)).append("]")
        append("}}],")
        append(""""outbounds":[{"tag":"direct","protocol":"freedom"},""")
        append(outboundJson).append("]")
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
