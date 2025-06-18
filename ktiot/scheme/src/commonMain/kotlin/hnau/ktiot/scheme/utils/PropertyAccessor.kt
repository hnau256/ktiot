package hnau.ktiot.scheme.utils

import hnau.common.logging.tryOrLog
import hnau.common.mqtt.utils.MqttClient
import hnau.ktiot.scheme.PropertyType
import hnau.ktiot.scheme.topic.MqttTopic
import hnau.ktiot.scheme.topic.raw
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger { }

class PropertyAccessor<T>(
    private val topic: MqttTopic.Absolute,
    @SerialName("property_type")
    val type: PropertyType<T>,
    private val client: MqttClient,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun subscribe(): Flow<T> = client
        .subscribe(
            topic = topic.raw,
        )
        .transformLatest { message ->
            logger
                .tryOrLog(
                    log = "parsing '$message' from $topic"
                ) {
                    Json.Default.decodeFromString(
                        deserializer = type.serializer,
                        string = message.payload.decodeToString(),
                    )
                }
                .onSuccess { parsed -> emit(parsed) }
        }

    suspend fun publish(
        value: T,
        retained: Boolean,
    ): Boolean {
        val payload = logger
            .tryOrLog(
                log = "encoding '$value' for $topic"
            ) {
                Json.Default
                    .encodeToString(
                        serializer = type.serializer,
                        value = value
                    )
                    .encodeToByteArray()
            }
            .getOrNull()
            ?: return false

        return client.publish(
            topic = topic.raw,
            payload = payload,
            retained = retained,
        )
    }
}