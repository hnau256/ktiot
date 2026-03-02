package hnau.common.mqtt.utils

data class MqttOperationError(
    val cause: Throwable?,
    val type: Type,
) {
    enum class Type { NetworkError, BrokerRefused }
}
