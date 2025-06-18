package hnau.ktiot.coordinator.utils

import hnau.common.logging.tryOrLog
import hnau.common.mqtt.utils.MqttClient
import hnau.common.mqtt.utils.Topic
import hnau.ktiot.scheme.PropertyType
import hnau.ktiot.scheme.topic.ChildTopic
import hnau.ktiot.scheme.topic.MqttTopic
import hnau.ktiot.scheme.topic.raw
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger { }

fun RawProperty(
    topic: ChildTopic,
    client: MqttClient,
): RawProperty = object : RawProperty {

    override val topic: ChildTopic
        get() = topic

    private val rawTopic: Topic = topic.topic.raw

    override fun subscribe(): Flow<ByteArray> = client
        .subscribe(
            topic = rawTopic,
        )
        .map { message -> message.payload }

    override suspend fun publish(
        payload: ByteArray,
        retained: Boolean,
    ): Boolean = client.publish(
        topic = rawTopic,
        payload = payload,
        retained = retained,
    )
}

fun <T, P : PropertyType<T>> RawProperty.typed(
    type: P,
): Property<T, P> = object : Property<T, P> {

    private val source: RawProperty
        get() = this@typed

    override val topic: ChildTopic
        get() = source.topic

    override val type: P
        get() = type

    override fun subscribe(): Flow<T> = source
        .subscribe()
        .transformLatest { payload ->
            logger
                .tryOrLog(
                    log = "parse mqtt payload to typed json",
                ) {
                    Json.decodeFromString(
                        deserializer = type.serializer,
                        string = payload.decodeToString()
                    )
                }
                .onSuccess { emit(it) }
        }

    override suspend fun publish(
        payload: T,
        retained: Boolean,
    ): Boolean {
        val payloadBytes = logger
            .tryOrLog(
                log = "encode typed payload to mqtt payload",
            ) {
                Json
                    .encodeToString(
                        serializer = type.serializer,
                        value = payload,
                    )
                    .encodeToByteArray()
            }
            .getOrNull()
            ?: return false
        return source.publish(
            payload = payloadBytes,
            retained = retained,
        )
    }

}