package org.hnau.commons.mqtt

interface MqttClient {
    suspend fun connect(
        config: MqttConfig,
        block: suspend MqttSession.() -> Nothing,
    ): MqttResult
}
