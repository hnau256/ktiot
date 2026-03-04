package hnau.common.mqtt

import hnau.common.mqtt.types.Message
import hnau.common.mqtt.types.QoS
import hnau.common.mqtt.types.topic.Topic
import hnau.common.mqtt.types.MqttResult
import hnau.common.mqtt.platform.MqttSimpleSession
import hnau.common.mqtt.platform.doAsync
import hnau.common.mqtt.platform.idMapper
import hnau.common.mqtt.platform.toMqttError
import hnau.common.mqtt.platform.toMqttResult
import hnau.common.mqtt.utils.raw
import hnau.common.mqtt.utils.rawMapper
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttMessage

internal class JvmMqttSimpleSession(
    private val client: IMqttAsyncClient,
    messagesBufferSize: Int,
    onDisconnected: (MqttResult.Error) -> Unit,
) : MqttSimpleSession {

    override val messages: MutableSharedFlow<Pair<Topic.Absolute, Message>> = MutableSharedFlow(
        extraBufferCapacity = messagesBufferSize,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        client.setCallback(
            object : MqttCallback {

                override fun connectionLost(cause: Throwable) {
                    onDisconnected(cause.toMqttError())
                }

                override fun messageArrived(
                    topic: String,
                    message: MqttMessage,
                ) {
                    val typedTopic = Topic.Absolute.rawMapper.direct(topic)
                    val message = Message(
                        id = Message.Id(message.id),
                        payload = message.payload,
                        retained = message.isRetained,
                        qoS = QoS.idMapper.direct(message.qos),
                    )
                    messages.tryEmit(typedTopic to message)
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
            },
        )
    }

    override suspend fun subscribe(
        topic: Topic.Absolute,
        qoS: QoS,
    ): MqttResult<Unit> = doMqttOperation {
        doAsync {
            subscribe(
                /* topicFilter = */ topic.raw,
                /* qos = */ QoS.idMapper.reverse(qoS),
            )
        }.toMqttResult()
    }

    override suspend fun unsubscribe(
        topic: Topic.Absolute,
    ): MqttResult<Unit> = doMqttOperation {
        doAsync {
            unsubscribe(
                /* topicFilter = */ topic.raw,
            )
        }.toMqttResult()
    }

    override suspend fun publish(
        topic: Topic.Absolute,
        payload: ByteArray,
        retained: Boolean,
        qoS: QoS,
    ): MqttResult<Unit> = doMqttOperation {
        doAsync {
            publish(
                /* topic = */ topic.raw,
                /* payload = */ payload,
                /* qos = */ QoS.idMapper.reverse(qoS),
                /* retained = */ retained,
            )
        }.toMqttResult()
    }

    private val mqttOperationMutex = Mutex()

    private suspend fun <R> doMqttOperation(
        block: suspend IMqttAsyncClient.() -> R,
    ): R = mqttOperationMutex.withLock {
        client.block()
    }
}
