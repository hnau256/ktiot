package hnau.common.mqtt.types

import kotlin.time.Instant

sealed interface MqttState {

    data object Connecting: MqttState

    data class Connected(
        val session: MqttSession,
    ): MqttState

    data class WaitingForReconnect(
        val disconnectedError: MqttResult.Error,
        val nextAttemptAt: Instant,
        val connectNow: () -> Unit,
    ): MqttState
}