package org.hnau.ktiot.mqtt.types

import org.hnau.commons.gen.fold.annotations.Fold

sealed interface MqttResult<out T> {

    data class Success<out T>(
        val value: T,
    ): MqttResult<T>

    data class Error(
        val cause: Throwable,
        val type: Type,
    ): MqttResult<Nothing> {

        @Fold
        enum class Type { Network, Broker }
    }
}