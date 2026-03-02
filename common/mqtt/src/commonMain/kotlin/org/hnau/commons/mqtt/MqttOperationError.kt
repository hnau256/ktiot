package org.hnau.commons.mqtt

data class MqttOperationError(
    val cause: Throwable?,
    val type: Type,
) {
    enum class Type { NetworkError, BrokerRefused }
}
