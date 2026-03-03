package hnau.common.mqtt.types

import kotlin.time.Instant

interface MqttState {

    data class Connecting(
        val started: Instant,
    ): MqttState

    data class Connected(
        val session: MqttSession,
    ): MqttState

    data class WaitingForReconnect(
        val disconnectedError: MqttResult.Error,
        val nextAttemptAt: Instant,
        val connectNow: () -> Unit,
    ): MqttState
}