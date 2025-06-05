package hnau.common.mqtt.utils

import arrow.core.Option
import hnau.common.kotlin.coroutines.mapState
import hnau.common.kotlin.ifTrue
import hnau.common.logging.tryOrLog
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttMessage

private val logger = KotlinLogging.logger { }

class JvmMqttClient(
    private val scope: CoroutineScope,
    private val client: IMqttAsyncClient,
) : MqttClient {

    private data class Message(
        val topic: Topic,
        val message: JsonElement,
        val retained: Boolean,
    )

    private val messages: MutableSharedFlow<Message> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = Int.MAX_VALUE,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val subscriptions = HashMap<Topic, Flow<JsonElement>>()

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

    private suspend inline fun <R> tryOrLogInScope(
        log: String,
        crossinline block: suspend () -> R,
    ): Option<R> = scope
        .async {
            logger.tryOrLog(
                log = log,
                block = { block() },
            )
        }
        .await()

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
                    val messageString = logger.tryOrLog(
                        log = "decoding ByteArray to String (message from $topic)"
                    ) {
                        message.payload.decodeToString()
                    }.getOrNull() ?: return

                    val messageJson = logger.tryOrLog(
                        log = "parsing '$messageString' to JsonElement (message from $topic)"
                    ) {
                        Json.Default.parseToJsonElement(messageString)
                    }.getOrNull() ?: return

                    messages.tryEmit(
                        value = Message(
                            topic = Topic(topic),
                            message = messageJson,
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
    ): Flow<JsonElement> = synchronized(subscriptions) {
        subscriptions.getOrPut(topic) {
            var lastRetainedMessage: JsonElement? = null
            flow {
                try {
                    doSubscribe(topic, qoS)
                    messages
                        .filter { it.topic == topic }
                        .collect{message ->
                            message
                                .retained
                                .ifTrue { lastRetainedMessage = message.message }
                            emit(message.message)
                        }
                } finally {
                    lastRetainedMessage = null
                    doUnsubscribe(topic)
                }
            }
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
        value: JsonElement,
        retained: Boolean,
    ): Boolean {
        val topicString = topic.topic

        val messageString = logger.tryOrLog(
            log = "encoding '$value' to String (message for $topic)"
        ) {
            Json.Default.encodeToString(value)
        }.getOrNull() ?: return false

        val message = logger.tryOrLog(
            log = "decoding ByteArray to String (message for $topic)"
        ) {
            messageString.encodeToByteArray()
        }.getOrNull() ?: return false

        val log = "publishing to `$topicString`: '$messageString'"
        return pushOperationAndWait(
            log = log,
        ) {
            logger.tryOrLog(
                log = log,
            ) {
                publish(
                    /* topic = */ topicString,
                    /* payload = */ message,
                    /* qos = */ qoS.code,
                    /* retained = */ retained
                )
            }
        }.isSome()
    }
}