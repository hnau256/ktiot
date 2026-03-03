package hnau.common.mqtt.utils

import co.touchlab.kermit.Logger
import hnau.common.mqtt.types.MqttResult

internal fun Logger.logMqttError(
    action: String,
    error: MqttResult.Error,
) {
    w(
        throwable = error.cause,
    ) {
        val errorType = when (error.type) {
            MqttResult.Error.Type.Network -> "Network"
            MqttResult.Error.Type.Broker -> "Broker"
        }
        "$errorType mqtt error while $action"
    }
}