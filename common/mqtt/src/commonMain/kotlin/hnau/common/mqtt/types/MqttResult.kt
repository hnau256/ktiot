package hnau.common.mqtt.types

sealed interface MqttResult<out T> {

    data class Success<out T>(
        val value: T,
    ): MqttResult<T>

    data class Error(
        val cause: Throwable,
        val type: Type,
    ): MqttResult<Nothing> {
        enum class Type { Network, Broker }
    }
}