package hnau.common.mqtt.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonElement

interface MqttClient {

    fun subscribe(
        topic: Topic,
        qoS: QoS = QoS.default,
    ): Flow<JsonElement>

    suspend fun publish(
        topic: Topic,
        qoS: QoS = QoS.default,
        value: JsonElement,
        retained: Boolean,
    ): Boolean

    companion object
}