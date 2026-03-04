package hnau.common.mqtt.utils

import hnau.common.mqtt.platform.MqttSimpleSession
import hnau.common.mqtt.types.Message
import hnau.common.mqtt.types.MqttResult
import hnau.common.mqtt.types.QoS
import hnau.common.mqtt.types.topic.Topic
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.hnau.commons.gen.loggable.annotations.Loggable
import org.hnau.commons.kotlin.coroutines.flow.state.mutable.toMutableStateFlowAsInitial
import org.hnau.commons.kotlin.ifTrue
import kotlin.time.Duration

@Loggable
internal class TopicSubscriptionHolder(
    scope: CoroutineScope,
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
            val unsubscribed = unsubscribeIfSubscribed()
            if (unsubscribed) {
                cachedRetainedMessage.value = null
            }
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

    private suspend fun unsubscribeIfSubscribed(): Boolean = subscribeUnsubscribeMutex.withLock {

        if (!isSubscribed) {
            return@withLock true
        }

        val result = simpleSession.unsubscribe(
            topic = topic,
        )

        val unsubscribed = when (result) {
            is MqttResult.Error -> {
                val topic = Topic.Absolute.stringMapper.reverse(topic)
                /*logger.logMqttError(
                    action = "unsubscribing from topic '$topic'",
                    error = result,
                )*/ //TODO("Uncomment")
                false
            }

            is MqttResult.Success<*> -> true
        }

        isSubscribed = !unsubscribed

        unsubscribed
    }

    private suspend fun subscribeIfNotSubscribed() {
        subscribeUnsubscribeMutex.withLock {

            if (isSubscribed) {
                return@withLock
            }

            val result = simpleSession.subscribe(
                topic = topic,
                qoS = qoS,
            )

            isSubscribed = when (result) {
                is MqttResult.Error -> {
                    val topic = Topic.Absolute.stringMapper.reverse(topic)
                    /*logger.logMqttError(
                        action = "subscribing to topic '$topic'",
                        error = result,
                    )*/ //TODO("Uncomment")
                    false
                }

                is MqttResult.Success<*> -> true
            }
        }
    }
}