package hnau.common.mqtt.utils

import hnau.common.mqtt.types.BrokerConfig

internal interface MqttClient {

    suspend fun <T> connect(
        config: BrokerConfig,
        block: suspend MqttSession.() -> T,
    ): MqttResult<T>
}