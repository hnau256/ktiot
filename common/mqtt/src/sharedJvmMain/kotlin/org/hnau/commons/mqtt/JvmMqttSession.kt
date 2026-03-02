package org.hnau.commons.mqtt

import arrow.core.Either
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.hnau.commons.mqtt.internal.MqttOperationError
import org.hnau.commons.mqtt.internal.MqttSession
import org.hnau.commons.mqtt.utils.PahoOperation
import org.hnau.commons.mqtt.utils.toMessage

internal class JvmMqttSession(
    private val pahoClient: IMqttAsyncClient,
    mqttConfig: MqttConfig,
) : MqttSession {
    private val _disconnectDeferred = CompletableDeferred<Throwable?>()
    val disconnectDeferred: Deferred<Throwable?> get() = _disconnectDeferred
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val incomingMessages =
        MutableSharedFlow<Pair<String, MqttMessage>>(
            extraBufferCapacity = mqttConfig.messageBufferSize,
        )

    private val operationChannel = Channel<PahoOperation>(capacity = Channel.UNLIMITED)

    init {
        pahoClient.setCallback(
            object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    _disconnectDeferred.complete(cause)
                }

                override fun messageArrived(
                    topic: String,
                    message: MqttMessage,
                ) {
                    incomingMessages.tryEmit(topic to message)
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
            },
        )

        scope.launch {
            for (operation in operationChannel) {
                operation.result.complete(operation.type.execute(pahoClient))
            }
        }
    }

    override suspend fun subscribe(
        topic: Topic,
        qoS: QoS,
    ): Either<MqttOperationError, Flow<Message>> {
        val deferred = CompletableDeferred<Either<MqttOperationError, Unit>>()
        operationChannel.send(
            PahoOperation(
                type = PahoOperation.Type.Subscribe(topic = topic.value, qoS = qoS.mqttCode),
                result = deferred,
            ),
        )
        return deferred.await().map {
            callbackFlow {
                incomingMessages
                    .filter { (t, _) -> t == topic.value }
                    .map { (_, msg) -> msg.toMessage() }
                    .collect { send(it) }

                awaitClose {
                    scope.launch {
                        operationChannel.send(
                            PahoOperation(
                                type = PahoOperation.Type.Unsubscribe(topic = topic.value),
                                result = CompletableDeferred(),
                            ),
                        )
                    }
                }
            }.buffer(capacity = Channel.UNLIMITED)
        }
    }

    override suspend fun publish(
        topic: Topic,
        payload: ByteArray,
        qoS: QoS,
        retained: Boolean,
    ): Either<MqttOperationError, Unit> {
        val deferred = CompletableDeferred<Either<MqttOperationError, Unit>>()
        operationChannel.send(
            PahoOperation(
                type =
                    PahoOperation.Type.Publish(
                        topic = topic.value,
                        payload = payload,
                        qoS = qoS.mqttCode,
                        retained = retained,
                    ),
                result = deferred,
            ),
        )
        return deferred.await()
    }

    fun close() {
        val sessionClosed = CancellationException("Session closed")
        operationChannel.close(sessionClosed)
        while (true) {
            val operation = operationChannel.tryReceive().getOrNull() ?: break
            operation.result.completeExceptionally(sessionClosed)
        }
        scope.cancel()
    }
}
