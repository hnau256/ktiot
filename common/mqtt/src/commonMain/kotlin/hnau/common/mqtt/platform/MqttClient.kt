package hnau.common.mqtt.platform

import hnau.common.mqtt.types.BrokerConfig

internal interface MqttClient {

    suspend fun <T> connect(
        config: BrokerConfig,
        block: suspend MqttSession.() -> T,
    ): MqttResult<T>
}