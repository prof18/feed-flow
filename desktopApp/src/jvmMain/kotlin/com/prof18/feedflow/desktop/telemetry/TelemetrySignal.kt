package com.prof18.feedflow.desktop.telemetry

import kotlinx.serialization.Serializable

@Serializable
data class TelemetrySignal(
    val appID: String,
    val clientUser: String,
    val type: String,
    val sessionID: String,
    val isTestMode: Boolean = false,
    val payload: Map<String, String> = emptyMap(),
)
