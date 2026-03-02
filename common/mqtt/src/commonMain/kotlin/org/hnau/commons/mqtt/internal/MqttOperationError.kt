package org.hnau.commons.mqtt.internal

internal data class MqttOperationError(
    val cause: Throwable?,
    val type: Type,
) {
    enum class Type { NetworkError, BrokerRefused }
}
