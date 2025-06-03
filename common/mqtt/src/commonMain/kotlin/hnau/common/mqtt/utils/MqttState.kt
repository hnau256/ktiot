package hnau.common.mqtt.utils

import kotlinx.datetime.Instant

sealed interface MqttState {

    data object Connecting : MqttState

    data class WaitingForReconnection(
        val cause: Throwable,
        val reconnectionAt: Instant,
        val reconnectNow: () -> Unit,
    ) : MqttState

    data class Connected(
        val client: MqttClient,
    ) : MqttState
}