package hnau.ktiot.coordinator.property

import hnau.ktiot.scheme.PropertyType
import hnau.ktiot.scheme.topic.MqttTopic
import kotlinx.coroutines.flow.Flow

interface Property<T, P : PropertyType<T>> {

    val topic: MqttTopic.Absolute

    val type: P

    fun subscribe(): Flow<T>

    suspend fun publish(
        payload: T,
        retained: Boolean,
    ): Boolean
}