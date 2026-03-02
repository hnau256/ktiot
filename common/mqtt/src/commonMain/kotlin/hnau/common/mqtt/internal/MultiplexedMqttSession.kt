package hnau.common.mqtt.internal

import arrow.core.Either
import hnau.common.mqtt.utils.Message
import hnau.common.mqtt.utils.MqttOperationError
import hnau.common.mqtt.utils.QoS
import hnau.common.mqtt.utils.Topic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration

internal class MultiplexedMqttSession(
    private val session: MqttSession,
    private val scope: CoroutineScope,
    private val subscriptionStopDelay: Duration,
) : MqttSession {
    private val subscriptions = mutableMapOf<String, Deferred<Either<MqttOperationError, SharedFlow<Message>>>>()
    private val mutex = Mutex()

    override suspend fun subscribe(
        topic: Topic,
        qoS: QoS,
    ): Either<MqttOperationError, Flow<Message>> {
        val deferred =
            mutex.withLock {
                subscriptions.getOrPut(topic.value) {
                    scope.async {
                        session
                            .subscribe(topic = topic, qoS = qoS)
                            .also { result ->
                                if (result.isLeft()) mutex.withLock { subscriptions.remove(topic.value) }
                            }.map { upstream ->
                                val retainedCache = MutableStateFlow<Message?>(null)
                                upstream
                                    .onEach { msg ->
                                        retainedCache.value = if (msg.retained) msg else null
                                    }.onCompletion {
                                        mutex.withLock { subscriptions.remove(topic.value) }
                                    }.shareIn(
                                        scope = scope,
                                        started =
                                            SharingStarted.WhileSubscribed(
                                                stopTimeoutMillis = subscriptionStopDelay.inWholeMilliseconds,
                                            ),
                                        replay = 0,
                                    ).onSubscription {
                                        retainedCache.value?.let { emit(it) }
                                    }
                            }
                    }
                }
            }
        return deferred.await()
    }

    override suspend fun publish(
        topic: Topic,
        payload: ByteArray,
        qoS: QoS,
        retained: Boolean,
    ): Either<MqttOperationError, Unit> =
        session.publish(
            topic = topic,
            payload = payload,
            qoS = qoS,
            retained = retained,
        )
}
