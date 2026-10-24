package dev.tunnelkit.core.model

/** Identity of an integration, not of a protocol. */
@JvmInline
value class EngineId(val value: String) {
    override fun toString(): String = value
}

/**
 * Typed connection state. The brief forbids boolean soup: every transition is a value,
 * and every failure is a typed case that the UI can map to an actionable message.
 */
sealed interface ConnectionState {
    data object Idle : ConnectionState
    data object Validating : ConnectionState
    data class Preparing(val engine: EngineId) : ConnectionState
    data class Discovering(val engine: EngineId) : ConnectionState
    data class Connecting(val engine: EngineId) : ConnectionState
    data class Connected(val engine: EngineId, val sinceEpochMillis: Long) : ConnectionState
    data class Reconnecting(val engine: EngineId, val attempt: Int) : ConnectionState
    data object Stopping : ConnectionState
    data object Stopped : ConnectionState
    data class Failed(val failure: Failure, val engine: EngineId? = null) : ConnectionState

    /** Terminal, requires the user or the owner to act (license, credentials, permission). */
    data class Blocked(val failure: Failure, val engine: EngineId? = null) : ConnectionState

    fun isActive(): Boolean = this is Connected || this is Connecting ||
        this is Preparing || this is Discovering || this is Reconnecting
}

/** Typed failures, one per class of real-world cause. */
sealed class Failure(val code: String) {
    open val detail: String get() = ""

    data object InvalidConfig : Failure("invalid_config")
    data class UnsupportedFeature(val feature: String) : Failure("unsupported_feature") {
        override val detail: String get() = feature
    }
    data class MissingCredentials(val what: String) : Failure("missing_credentials") {
        override val detail: String get() = what
    }
    data class MissingBootstrap(val what: String) : Failure("missing_bootstrap") {
        override val detail: String get() = what
    }
    data class DnsFailure(override val detail: String) : Failure("dns_failure")
    data class TlsFailure(override val detail: String) : Failure("tls_failure")
    data class AuthenticationFailure(override val detail: String) : Failure("authentication_failure")
    data class EndpointUnreachable(override val detail: String) : Failure("endpoint_unreachable")
    data class Timeout(override val detail: String) : Failure("timeout")
    data class PermissionDenied(override val detail: String) : Failure("permission_denied")
    data object LicenseBlocked : Failure("license_blocked")
    data class NativeCrash(override val detail: String) : Failure("native_crash")
    data class Unknown(override val detail: String) : Failure("unknown")

    override fun toString(): String = if (detail.isEmpty()) code else "$code: $detail"
}

/** Counters a UI or a health check can watch. Never contains addresses or credentials. */
data class EngineMetrics(
    val txPackets: Long = 0,
    val rxPackets: Long = 0,
    val txBytes: Long = 0,
    val rxBytes: Long = 0,
    val lastErrorCode: String? = null,
)

data class DiagnosticLine(val level: Level, val message: String) {
    enum class Level { DEBUG, INFO, WARN, ERROR }
}
