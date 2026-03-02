package hnau.common.mqtt.utils

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class MqttConfig(
    val broker: MqttBrokerConfig,
    val reconnect: ReconnectPolicy = ReconnectPolicy(),
    val messageBufferSize: Int = 64,
    val subscriptionStopDelay: Duration = 5.seconds,
)
