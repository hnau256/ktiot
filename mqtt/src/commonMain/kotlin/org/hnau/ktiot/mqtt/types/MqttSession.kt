package org.hnau.ktiot.mqtt.types

import org.hnau.ktiot.mqtt.types.topic.Topic
import kotlinx.coroutines.flow.Flow

interface MqttSession {

    fun subscribe(
        topic: Topic.Absolute,
        qoS: QoS = QoS.default,
    ): Flow<Message>

    suspend fun publish(
        topic: Topic.Absolute,
        payload: ByteArray,
        retained: Boolean,
        qoS: QoS = QoS.default,
    ): MqttResult<Unit>
}