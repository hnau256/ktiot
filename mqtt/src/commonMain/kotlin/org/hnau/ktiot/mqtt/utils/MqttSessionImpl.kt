package org.hnau.ktiot.mqtt.utils

import org.hnau.ktiot.mqtt.platform.MqttSimpleSession
import org.hnau.ktiot.mqtt.types.Message
import org.hnau.ktiot.mqtt.types.MqttResult
import org.hnau.ktiot.mqtt.types.MqttSession
import org.hnau.ktiot.mqtt.types.QoS
import org.hnau.ktiot.mqtt.types.topic.Topic
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