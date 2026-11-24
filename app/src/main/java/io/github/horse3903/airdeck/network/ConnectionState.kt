package io.github.horse3903.airdeck.network


data class ConnectionState(
    val connected: Boolean = false,
    val ipAddress: String = "",
    val port: Int = -1,
    val error: String? = null,
    val isConnecting: Boolean = false,
    val controllerSlot: Int? = null,
    val isRunningDiagnostics: Boolean = false,
    val diagnosticResults: List<NetworkDiagnostics.DiagnosticResult> = emptyList()
)
