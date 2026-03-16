package hnau.common.mqtt.platform

import hnau.common.mqtt.types.MqttResult

internal interface MqttClient {

    suspend fun connect(
        block: suspend (session: MqttSimpleSession) -> Nothing,
    ): MqttResult.Error
}