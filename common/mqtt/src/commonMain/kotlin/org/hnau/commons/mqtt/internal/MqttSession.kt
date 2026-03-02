package hnau.common.mqtt.internal

import arrow.core.Either
import kotlinx.coroutines.flow.Flow
import hnau.common.mqtt.Message
import hnau.common.mqtt.QoS
import hnau.common.mqtt.Topic

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
