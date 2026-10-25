package dev.tunnelkit.core.profile

data class Profile(
    val id: String,
    val name: String,
    val link: String,
    val transport: String = "",
    val security: String = "",
    /** "vless" | "wireguard" — decides which engine config the service builds. */
    val kind: String = "vless",
) {
    /** Never log [link] directly: it carries the UUID. */
    fun redacted(): String = "$name ($transport/$security)"
}
