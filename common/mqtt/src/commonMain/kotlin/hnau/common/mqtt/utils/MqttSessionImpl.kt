package hnau.common.mqtt.utils

import hnau.common.mqtt.platform.MqttSimpleSession
import hnau.common.mqtt.types.Message
import hnau.common.mqtt.types.MqttResult
import hnau.common.mqtt.types.MqttSession
import hnau.common.mqtt.types.QoS
import hnau.common.mqtt.types.topic.Topic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

internal class MqttSessionImpl(
    private val scope: CoroutineScope,
    private val simple: MqttSimpleSession,
    private val unsubscribeDelay: Duration,
) : MqttSession {

    private val subscriptionHoldersSyncObject = SynchronizedObject()
    private val subscriptionHolders: MutableMap<Topic.Absolute, TopicSubscriptionHolder> =
        mutableMapOf()

    override fun subscribe(
        topic: Topic.Absolute,
        qoS: QoS
    ): Flow<Message> = synchronized(subscriptionHoldersSyncObject) {
        subscriptionHolders.getOrPut(topic) {
            TopicSubscriptionHolder(
                scope = scope,
                topic = topic,
                qoS = qoS,
                simpleSession = simple,
                unsubscribeDelay = unsubscribeDelay,
            )
        }
    }.messages

    override suspend fun publish(
        topic: Topic.Absolute,
        payload: ByteArray,
        retained: Boolean,
        qoS: QoS
    ): MqttResult<Unit> = simple.publish(
        topic = topic,
        payload = payload,
        retained = retained,
        qoS = qoS,
    )
}