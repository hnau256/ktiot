package hnau.common.mqtt.utils

data class MqttConfig(
    val broker: MqttBrokerConfig,
    val reconnect: ReconnectPolicy = ReconnectPolicy(),
    val messageBufferSize: Int = 64,
)
