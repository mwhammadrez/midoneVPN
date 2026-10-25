package dev.tunnelkit.core.engine

import dev.tunnelkit.core.model.DiagnosticLine
import dev.tunnelkit.core.model.EngineId
import dev.tunnelkit.core.model.EngineMetrics

/** Formats the app can hand to an engine. Detection is explicit, never guessed silently. */
enum class ConfigFormat {
    VLESS_LINK,
    SHARE_LINK,
    XRAY_JSON,
    SUBSCRIPTION_TEXT,
    DNS_TUNNEL_LINK,
    WIREGUARD_CONF,
    OPENVPN_PROFILE,
}

/** Who owns the TUN file descriptor. Chaining two owners is forbidden. */
enum class TunOwnership { ENGINE_OWNS_TUN, LOCAL_PROXY, APP_TUN_FD_PASSED }

/** Integration status as required by the brief — no optimistic values. */
enum class IntegrationStatus {
    IMPLEMENTED,
    IMPLEMENTED_BUT_UNVERIFIED,
    BLOCKED_LICENSE,
    BLOCKED_CREDENTIALS,
    BLOCKED_ENVIRONMENT,
    NOT_STARTED,
}

data class EngineDescriptor(
    val id: EngineId,
    /** What we integrated, e.g. "libXray". Never the name of an app we did not licence. */
    val engineName: String,
    val engineVersion: String,
    val provenanceUrl: String,
    val provenanceCommit: String,
    val licenseId: String,
    val formats: Set<ConfigFormat>,
    val tunOwnership: TunOwnership,
    val supportsUdp: Boolean,
    /** Data the server side must supply before this engine can ever connect. */
    val requires: Set<String> = emptySet(),
    val status: IntegrationStatus = IntegrationStatus.NOT_STARTED,
)

/** Engine-specific configuration, already normalised by the app. */
data class EngineConfig(
    val format: ConfigFormat,
    /** Raw or normalised payload (link, JSON, conf). Never logged. */
    val payload: String,
    /** Non-secret, user-visible label used in the UI and in redacted diagnostics. */
    val label: String = "",
    /** Runtime-only values injected at connect time (e.g. the TUN fd). Never persisted. */
    val runtime: Map<String, String> = emptyMap(),
) {
    companion object {
        const val RUNTIME_TUN_FD = "tun.fd"

        /** Name of the interface behind the fd; Xray refuses the fd when it does not match. */
        const val RUNTIME_TUN_NAME = "tun.name"
    }
}

data class ValidationResult(val ok: Boolean, val reason: String = "") {
    companion object {
        val Valid = ValidationResult(true)
        fun invalid(reason: String) = ValidationResult(false, reason)
    }
}

/** Thrown when an engine reports a typed failure (as opposed to an unexpected throwable). */
class EngineFailureException(val failure: dev.tunnelkit.core.model.Failure) : Exception(failure.toString())

/** Thrown by engines that are present for their interface but must not run. */
class EngineBlockedException(message: String) : Exception(message)

/**
 * The single contract every engine adapter implements.
 * Adapters are thin: they translate [EngineConfig] into the upstream engine's native input,
 * call it, and map its callbacks/errors back into [dev.tunnelkit.core.model.ConnectionState]
 * / [dev.tunnelkit.core.model.Failure]. They do not re-implement tunneling.
 */
interface VpnEngine {

    val descriptor: EngineDescriptor

    /** Cheap, side-effect free checks (parseability, required fields, license gate). */
    suspend fun validate(config: EngineConfig): ValidationResult

    /** Allocate what start() will need (native library, local proxy port). Idempotent. */
    suspend fun prepare()

    /**
     * Establish the tunnel. Must be idempotent while already started, and must release
     * everything it acquired if it throws.
     */
    suspend fun start(config: EngineConfig)

    /** Stop and release: process/handles/threads/TUN-fd. Safe to call when not started. */
    suspend fun stop()

    fun metrics(): EngineMetrics = EngineMetrics()

    /** Redacted, bounded. Never includes keys, UUIDs, passwords or full URLs. */
    fun diagnostics(): List<DiagnosticLine> = emptyList()
}
