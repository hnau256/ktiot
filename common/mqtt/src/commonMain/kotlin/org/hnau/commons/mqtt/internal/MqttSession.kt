package org.hnau.commons.mqtt.internal

import arrow.core.Either
import kotlinx.coroutines.flow.Flow
import org.hnau.commons.mqtt.Message
import org.hnau.commons.mqtt.QoS
import org.hnau.commons.mqtt.Topic

internal interface MqttSession {
    suspend fun subscribe(
        topic: Topic,
        qoS: QoS = QoS.Default,
    ): Either<MqttOperationError, Flow<Message>>

    suspend fun publish(
        topic: Topic,
        payload: ByteArray,
        qoS: QoS = QoS.Default,
        retained: Boolean = false,
    ): Either<MqttOperationError, Unit>
}
