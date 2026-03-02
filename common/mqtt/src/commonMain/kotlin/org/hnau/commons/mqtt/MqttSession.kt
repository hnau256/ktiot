package org.hnau.commons.mqtt

import arrow.core.Either
import kotlinx.coroutines.flow.Flow

interface MqttSession {
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
