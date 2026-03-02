package hnau.common.mqtt.internal

import arrow.core.Either
import hnau.common.mqtt.utils.Message
import hnau.common.mqtt.utils.MqttActiveSession
import hnau.common.mqtt.utils.MqttOperationError
import hnau.common.mqtt.utils.QoS
import hnau.common.mqtt.utils.Topic
import kotlinx.coroutines.flow.Flow

internal class MqttActiveSessionAdapter(
    private val session: MqttSession,
) : MqttActiveSession {
    override suspend fun subscribe(
        topic: Topic,
        qoS: QoS,
    ): Either<MqttOperationError, Flow<Message>> = session.subscribe(topic = topic, qoS = qoS)

    override suspend fun publish(
        topic: Topic,
        payload: ByteArray,
        qoS: QoS,
        retained: Boolean,
    ): Either<MqttOperationError, Unit> = session.publish(topic = topic, payload = payload, qoS = qoS, retained = retained)
}
