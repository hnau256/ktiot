package hnau.ktiot.coordinator.utils

import hnau.ktiot.scheme.PropertyType
import hnau.ktiot.scheme.topic.ChildTopic
import kotlinx.coroutines.flow.Flow

interface Property<T, P: PropertyType<T>> {

    val topic: ChildTopic

    val type: P

    fun subscribe(): Flow<T>

    suspend fun publish(
        payload: T,
        retained: Boolean,
    ): Boolean
}