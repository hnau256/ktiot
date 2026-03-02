package org.hnau.commons.mqtt

data class MqttConfig(
    val broker: MqttBrokerConfig,
    val reconnect: ReconnectPolicy = ReconnectPolicy(),
    val messageBufferSize: Int = 64,
)
