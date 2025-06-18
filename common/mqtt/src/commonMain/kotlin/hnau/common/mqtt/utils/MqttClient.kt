package hnau.common.mqtt.utils

import kotlinx.coroutines.flow.Flow

interface MqttClient {

    fun subscribe(
        topic: Topic,
        qoS: QoS = QoS.default,
    ): Flow<Message>

    suspend fun publish(
        topic: Topic,
        qoS: QoS = QoS.default,
        payload: ByteArray,
        retained: Boolean,
    ): Boolean

    companion object
}