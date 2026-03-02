package org.hnau.commons.mqtt.internal

import org.hnau.commons.mqtt.MqttBrokerConfig

internal interface MqttClient {
    suspend fun connect(
        config: MqttBrokerConfig,
        block: suspend MqttSession.() -> Nothing,
    ): MqttResult
}
