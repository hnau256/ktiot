package hnau.common.mqtt.internal

import hnau.common.mqtt.utils.MqttBrokerConfig

internal interface MqttClient {
    suspend fun connect(
        config: MqttBrokerConfig,
        block: suspend MqttSession.() -> Nothing,
    ): MqttResult
}
