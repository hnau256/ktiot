package org.hnau.commons.mqtt.internal

internal sealed interface MqttResult {
    sealed interface UnableToConnect : MqttResult {
        data class NetworkError(
            val cause: Throwable?,
        ) : UnableToConnect

        data class BrokerRefused(
            val cause: Throwable?,
        ) : UnableToConnect
    }

    sealed interface Disconnected : MqttResult {
        data class NetworkError(
            val cause: Throwable?,
        ) : Disconnected

        data class BrokerDisconnected(
            val cause: Throwable?,
        ) : Disconnected

        data object KeepAliveTimeout : Disconnected
    }
}
