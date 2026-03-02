package hnau.common.mqtt.utils

import org.eclipse.paho.client.mqttv3.MqttException
import hnau.common.mqtt.internal.MqttResult

internal fun MqttException.toUnableToConnect(): MqttResult.UnableToConnect =
    when (reasonCode) {
        MqttException.REASON_CODE_INVALID_PROTOCOL_VERSION.toInt(),
        MqttException.REASON_CODE_INVALID_CLIENT_ID.toInt(),
        MqttException.REASON_CODE_BROKER_UNAVAILABLE.toInt(),
        MqttException.REASON_CODE_FAILED_AUTHENTICATION.toInt(),
        MqttException.REASON_CODE_NOT_AUTHORIZED.toInt(),
        -> MqttResult.UnableToConnect.BrokerRefused(cause = this)
        else -> MqttResult.UnableToConnect.NetworkError(cause = this)
    }

internal fun MqttException.toDisconnected(): MqttResult.Disconnected =
    when (reasonCode) {
        MqttException.REASON_CODE_CONNECTION_LOST.toInt() -> MqttResult.Disconnected.NetworkError(cause = this)
        MqttException.REASON_CODE_CLIENT_TIMEOUT.toInt() -> MqttResult.Disconnected.KeepAliveTimeout
        else -> MqttResult.Disconnected.BrokerDisconnected(cause = this)
    }
