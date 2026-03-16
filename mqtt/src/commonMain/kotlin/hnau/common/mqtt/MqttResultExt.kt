package hnau.common.mqtt

import co.touchlab.kermit.Logger
import hnau.common.mqtt.types.MqttResult

fun Logger.logMqttError(
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