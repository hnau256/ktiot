package hnau.common.mqtt.types

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class MqttConfig(
    val broker: BrokerConfig,
    val reconnect: ReconnectPolicy = ReconnectPolicy(),
    val messageBufferSize: Int = 64,
    val unsubscribeDelay: Duration = 5.seconds,
)