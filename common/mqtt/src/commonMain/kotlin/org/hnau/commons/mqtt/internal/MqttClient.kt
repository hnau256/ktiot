package hnau.common.mqtt.internal

import hnau.common.mqtt.MqttBrokerConfig

internal interface MqttClient {
    suspend fun connect(
        config: MqttBrokerConfig,
        block: suspend MqttSession.() -> Nothing,
    ): MqttResult
}
