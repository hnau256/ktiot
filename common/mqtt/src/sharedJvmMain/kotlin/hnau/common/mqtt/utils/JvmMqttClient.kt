package hnau.common.mqtt.utils

import hnau.common.kotlin.ifTrue
import hnau.common.logging.tryOrLog
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttMessage

private val logger = KotlinLogging.logger { }

class JvmMqttClient(
    private val scope: CoroutineScope,
    private val client: IMqttAsyncClient,
) : MqttClient {

    private val messages: MutableSharedFlow<Pair<Topic, Message>> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = Int.MAX_VALUE,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val subscriptions = HashMap<Topic, Flow<Message>>()

    private data class Operation(
        val log: String,
        val operation: suspend IMqttAsyncClient.() -> Unit,
    )

    private val operationsQueue: MutableSharedFlow<Operation> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = Int.MAX_VALUE,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private fun pushOperation(
        log: String,
        operation: suspend IMqttAsyncClient.() -> Unit,
    ) {
        operationsQueue.tryEmit(
            Operation(
                log = log,
                operation = operation,
            )
        )
    }

    private suspend fun <R> pushOperationAndWait(
        log: String,
        operation: suspend IMqttAsyncClient.() -> R,
    ): R {
        val deferred = CompletableDeferred<R>()
        pushOperation(
            log = log,
        ) {
            val result = operation()
            deferred.complete(result)
        }
        return deferred.await()
    }

    init {
        scope.launch {
            try {
                awaitCancellation()
            } catch (ex: CancellationException) {
                withContext(NonCancellable) {
                    logger.tryOrLog(
                        log = "disconnecting",
                    ) {
                        client.disconnect().await()
                    }
                }
                throw ex
            }
        }

        client.setCallback(
            object : MqttCallback {

                override fun connectionLost(
                    cause: Throwable,
                ) {
                    scope.cancel(
                        message = "Mqtt client was disconnected",
                        cause = cause,
                    )
                }

                override fun messageArrived(
                    topic: String,
                    message: MqttMessage,
                ) {
                    val topic = Topic(topic)
                    messages.tryEmit(
                        value = topic to Message(
                            id = message.id,
                            payload = message.payload,
                            retained = message.isRetained,
                        ),
                    )
                }

                override fun deliveryComplete(
                    token: IMqttDeliveryToken?,
                ) {
                }
            }
        )

        scope.launch {
            operationsQueue.collect { operation ->
                logger.debug { "Executing ${operation.log}" }
                operation.operation(client)
            }
        }
    }

    private fun doSubscribe(
        topic: Topic,
        qoS: QoS,
    ) {
        val topicString = topic.topic
        val log = "subscribing to `$topicString`"
        pushOperation(
            log = log
        ) {
            logger.tryOrLog(
                log = log,
            ) {
                client.subscribe(
                    topicString,
                    qoS.code,
                )
            }
        }
    }

    private fun doUnsubscribe(
        topic: Topic,
    ) {
        val topicString = topic.topic
        val log = "unsubscribing from `$topicString`"
        pushOperation(
            log = log
        ) {
            logger.tryOrLog(
                log = log,
            ) {
                client.unsubscribe(
                    topicString,
                )
            }
        }
    }


    override fun subscribe(
        topic: Topic,
        qoS: QoS,
    ): Flow<Message> = synchronized(subscriptions) {
        subscriptions.getOrPut(topic) {
            var lastRetainedMessage: Message? = null
            flow {
                try {
                    messages
                        .filter { (messageTopic) -> messageTopic == topic }
                        .collect { (_, message) ->
                            message
                                .retained
                                .ifTrue { lastRetainedMessage = message }
                            logger.debug { "Received from '${topic.topic}': '${message.payload.decodeToString()}'" }
                            emit(message)
                        }
                } finally {
                    lastRetainedMessage = null
                    doUnsubscribe(topic)
                }
            }
                .onStart { doSubscribe(topic, qoS) }
                .shareIn(
                    scope = scope,
                    started = SharingStarted.WhileSubscribed(),
                )
                .onSubscription {
                    lastRetainedMessage?.let { last -> emit(last) }
                }
        }
    }

    override suspend fun publish(
        topic: Topic,
        qoS: QoS,
        payload: ByteArray,
        retained: Boolean,
    ): Boolean {
        val topicString = topic.topic

        val log = "publishing to `$topicString`: '${payload.decodeToString()}'"
        return pushOperationAndWait(
            log = log,
        ) {
            logger.tryOrLog(
                log = log,
            ) {
                publish(
                    /* topic = */ topicString,
                    /* payload = */ payload,
                    /* qos = */ qoS.code,
                    /* retained = */ retained
                ).await()
            }
        }.isSuccess
    }
}