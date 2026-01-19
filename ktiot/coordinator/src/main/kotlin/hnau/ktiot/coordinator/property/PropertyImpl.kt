package hnau.ktiot.coordinator.property

import hnau.common.logging.tryOrLog
import hnau.common.mqtt.utils.MqttClient
import hnau.ktiot.scheme.PropertyType
import hnau.ktiot.scheme.topic.MqttTopic
import hnau.ktiot.scheme.topic.raw
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger { }

internal class PropertyImpl<T, P : PropertyType<T>>(
    private val client: MqttClient,
    override val topic: MqttTopic.Absolute,
    override val type: P,
) : Property<T, P> {

    private val rawTopic = topic.raw

    override fun subscribe(): Flow<T> = client
        .subscribe(
            topic = rawTopic,
        )
        .transformLatest { payload ->
            logger
                .tryOrLog(
                    log = "parse mqtt payload to typed json",
                ) {
                    Json.decodeFromString(
                        deserializer = type.serializer,
                        string = payload.payload.decodeToString(),
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
        return client.publish(
            topic = rawTopic,
            payload = payloadBytes,
            retained = retained,
        )
    }
}