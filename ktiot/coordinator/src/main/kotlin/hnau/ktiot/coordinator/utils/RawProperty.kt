package hnau.ktiot.coordinator.utils

import hnau.ktiot.scheme.topic.ChildTopic
import kotlinx.coroutines.flow.Flow

interface RawProperty {

    val topic: ChildTopic

    fun subscribe(): Flow<ByteArray>

    suspend fun publish(
        payload: ByteArray,
        retained: Boolean,
    ): Boolean
}