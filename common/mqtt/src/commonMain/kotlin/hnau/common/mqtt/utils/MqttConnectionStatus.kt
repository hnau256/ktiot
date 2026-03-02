package hnau.common.mqtt.utils

import kotlinx.datetime.Instant

sealed interface MqttConnectionStatus {
    data object Connecting : MqttConnectionStatus

    data class WaitingForReconnection(
        val nextAttemptAt: Instant,
        val connectNow: () -> Unit,
    ) : MqttConnectionStatus

    data class Connected(
        val session: MqttActiveSession,
    ) : MqttConnectionStatus
}
