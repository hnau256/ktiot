package hnau.common.mqtt.utils

import hnau.common.mqtt.types.Message
import hnau.common.mqtt.types.QoS
import hnau.common.mqtt.types.topic.Topic
import kotlinx.coroutines.flow.Flow

internal interface MqttSession {

    val messages: Flow<Pair<Topic.Absolute, Message>>

    suspend fun subscribe(
        topic: Topic.Absolute,
        qoS: QoS = QoS.default,
    ): MqttResult<Unit>

    suspend fun unsubscribe(
        topic: Topic.Absolute,
    ): MqttResult<Unit>

    suspend fun publish(
        topic: Topic.Absolute,
        payload: ByteArray,
        retained: Boolean,
        qoS: QoS = QoS.default,
    ): MqttResult<Unit>
}