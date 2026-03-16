package org.hnau.ktiot.mqtt.platform

import org.hnau.ktiot.mqtt.types.MqttResult
import org.eclipse.paho.client.mqttv3.MqttException
import org.hnau.commons.kotlin.foldBoolean

internal fun <T> Result<T>.toMqttResult(): MqttResult<T> = fold(
    onSuccess = { result -> MqttResult.Success(result) },
    onFailure = { throwable -> throwable.toMqttError() }
)

internal fun Throwable.toMqttError(): MqttResult.Error = MqttResult.Error(
    cause = this,
    type = run {
        val isBrokerError = when {
            this !is MqttException -> false
            reasonCode !in brokerErrorCodes -> false
            else -> true
        }
        isBrokerError.foldBoolean(
            ifTrue = { MqttResult.Error.Type.Broker },
            ifFalse = { MqttResult.Error.Type.Network },
        )
    }
)

private val brokerErrorCodes: Set<Int> = listOf(
    MqttException.REASON_CODE_INVALID_PROTOCOL_VERSION, // Protocol version not supported by broker
    MqttException.REASON_CODE_INVALID_CLIENT_ID,        // Client ID rejected by broker
    MqttException.REASON_CODE_BROKER_UNAVAILABLE,       // Broker unavailable to handle request
    MqttException.REASON_CODE_FAILED_AUTHENTICATION,    // Bad username or password
    MqttException.REASON_CODE_NOT_AUTHORIZED,           // Not authorized to perform operation
    MqttException.REASON_CODE_UNEXPECTED_ERROR,         // Unexpected error on broker side
    MqttException.REASON_CODE_SUBSCRIBE_FAILED,         // Subscribe rejected by broker
)
    .map(Short::toInt)
    .toSet()