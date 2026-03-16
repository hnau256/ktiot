package org.hnau.ktiot.mqtt.platform

import org.hnau.ktiot.mqtt.types.MqttResult

internal interface MqttClient {

    suspend fun connect(
        block: suspend (session: MqttSimpleSession) -> Nothing,
    ): MqttResult.Error
}