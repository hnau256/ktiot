package org.hnau.ktiot.mqtt.utils

import org.hnau.ktiot.mqtt.logMqttError
import org.hnau.ktiot.mqtt.platform.MqttSimpleSession
import org.hnau.ktiot.mqtt.types.Message
import org.hnau.ktiot.mqtt.types.MqttResult
import org.hnau.ktiot.mqtt.types.QoS
import org.hnau.ktiot.mqtt.types.topic.Topic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.hnau.commons.gen.loggable.annotations.Loggable
import org.hnau.commons.kotlin.coroutines.flow.state.mutable.toMutableStateFlowAsInitial
import org.hnau.commons.kotlin.ifTrue
import kotlin.time.Duration

@Loggable
internal class TopicSubscriptionHolder(
    private val scope: CoroutineScope,
    private val topic: Topic.Absolute,
    private val qoS: QoS,
    private val simpleSession: MqttSimpleSession,
    unsubscribeDelay: Duration,
) {

    private val cachedRetainedMessage: MutableStateFlow<Message?> =
        null.toMutableStateFlowAsInitial()

    private val subscribeUnsubscribeMutex = Mutex()

    private var isSubscribed = false

    val messages: Flow<Message> = simpleSession
        .messages
        .mapNotNull { (messageTopic, message) ->
            (messageTopic == topic).ifTrue { message }
        }
        .onCompletion {
            unsubscribeIfSubscribed(
                onUnsubscribed = {
                    cachedRetainedMessage.value = null
                }
            )
        }
        .onEach { message ->
            message
                .takeIf(Message::retained)
                ?.let(cachedRetainedMessage::value::set)
        }
        .shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(
                stopTimeout = unsubscribeDelay,
            ),
            replay = 0,
        )
        .onStart {
            cachedRetainedMessage.value?.let { ratainedMessage ->
                emit(ratainedMessage)
            }
            subscribeIfNotSubscribed()
        }

    private fun unsubscribeIfSubscribed(
        onUnsubscribed: () -> Unit,
    ) {
        doSubscribeUnsubscribeOperation {
            val unsubscribed = unsubscribeIfSubscribedAsync()
            if (unsubscribed) {
                onUnsubscribed()
            }
        }
    }

    private suspend fun unsubscribeIfSubscribedAsync(): Boolean {
        if (!isSubscribed) {
            return true
        }

        val result = simpleSession.unsubscribe(
            topic = topic,
        )

        val unsubscribed = when (result) {
            is MqttResult.Error -> {
                val topic = Topic.Absolute.stringMapper.reverse(topic)
                logger.logMqttError(
                    action = "unsubscribing from topic '$topic'",
                    error = result,
                )
                false
            }

            is MqttResult.Success<*> -> true
        }

        isSubscribed = !unsubscribed

        return unsubscribed
    }

    private fun subscribeIfNotSubscribed() {
        doSubscribeUnsubscribeOperation {

            if (isSubscribed) {
                return@doSubscribeUnsubscribeOperation
            }

            val result = simpleSession.subscribe(
                topic = topic,
                qoS = qoS,
            )

            isSubscribed = when (result) {
                is MqttResult.Error -> {
                    val topic = Topic.Absolute.stringMapper.reverse(topic)
                    logger.logMqttError(
                        action = "subscribing to topic '$topic'",
                        error = result,
                    )
                    false
                }

                is MqttResult.Success<*> -> true
            }
        }
    }

    private fun doSubscribeUnsubscribeOperation(
        operation: suspend () -> Unit,
    ) {
        scope.launch {
            subscribeUnsubscribeMutex.withLock {
                operation()
            }
        }
    }
}