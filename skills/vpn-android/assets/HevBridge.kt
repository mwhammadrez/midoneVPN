package dev.tunnelkit.core

/**
 * Thin JNI face over heiher/hev-socks5-tunnel (MIT).
 *
 * C side (see assets/hev_jni.c):
 *   JNIEXPORT jint JNICALL Java_dev_tunnelkit_core_HevBridge_init(JNIEnv*, jobject, jint fd)
 *
 * The native API (src/hev-socks5-tunnel.h) is:
 *   int  hev_socks5_tunnel_init(int tun_fd);
 *   int  hev_socks5_tunnel_run(void);     // blocks
 *   void hev_socks5_tunnel_stop(void);
 *   void hev_socks5_tunnel_fini(void);
 *   void hev_socks5_tunnel_stats(size_t*, size_t*, size_t*, size_t*);
 */
object HevBridge {

    init {
        System.loadLibrary("hev-socks5-tunnel")
    }

    external fun init(tunFd: Int): Int
    external fun run(): Int
    external fun stop()
    external fun fini()

    /** @return [txPackets, txBytes, rxPackets, rxBytes] */
    external fun stats(): LongArray

    /**
     * YAML the native side reads before init(). Keep MTU <= 1400 when the far end is
     * TCP-over-TLS (VLESS/Trojan/Reality): 1500 - IP - TCP - TLS - VLESS headers.
     */
    fun configYaml(
        socksPort: Int,
        mtu: Int = 1400,
        ipv4: String = "198.18.0.1",
        ipv6: String = "fc00::1",
        username: String? = null,
        password: String? = null,
        socksAddress: String = "127.0.0.1",
        udpRelay: String = "udp",
    ): String = buildString {
        appendLine("tunnel:")
        appendLine("  name: tun0")
        appendLine("  mtu: $mtu")
        appendLine("  ipv4: $ipv4")
        appendLine("  ipv6: '$ipv6'")
        appendLine("  icmp: reply")
        appendLine("socks5:")
        appendLine("  port: $socksPort")
        appendLine("  address: $socksAddress")
        appendLine("  udp: $udpRelay")
        if (!username.isNullOrBlank()) {
            appendLine("  username: '${escape(username)}'")
            appendLine("  password: '${escape(password ?: "")}'")
        }
    }

    private fun escape(value: String) = value.replace("'", "''")
}
