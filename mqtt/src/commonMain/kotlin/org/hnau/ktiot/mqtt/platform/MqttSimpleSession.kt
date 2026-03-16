package org.hnau.ktiot.mqtt.platform

import org.hnau.ktiot.mqtt.types.Message
import org.hnau.ktiot.mqtt.types.MqttResult
import org.hnau.ktiot.mqtt.types.QoS
import org.hnau.ktiot.mqtt.types.topic.Topic
import kotlinx.coroutines.flow.Flow

internal interface MqttSimpleSession {

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