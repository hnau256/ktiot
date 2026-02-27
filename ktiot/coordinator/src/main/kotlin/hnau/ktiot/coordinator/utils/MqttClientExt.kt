package hnau.ktiot.coordinator.utils

import org.hnau.commons.kotlin.Loadable
import org.hnau.commons.kotlin.Loading
import org.hnau.commons.kotlin.Ready
import org.hnau.commons.mqtt.utils.MqttClient
import org.hnau.commons.mqtt.utils.QoS
import org.hnau.commons.mqtt.utils.Topic
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger { }

fun <T> MqttClient.subscribeJson(
    scope: CoroutineScope,
    topic: Topic,
    deserializer: KSerializer<T>,
    typeDescription: String,
    qoS: QoS = QoS.default,
): StateFlow<Loadable<T>> = subscribe(
    topic = topic,
    qoS = qoS,
)
    .transform { message ->
        message
            .payload
            .toString(Charsets.UTF_8)
            .let { encoded ->
                try {
                    val value = json.decodeFromString(
                        deserializer = deserializer,
                        string = encoded
                    )
                    emit(value)
                } catch (th: Throwable) {
                    logger.warn(th) { "Unable parse $typeDescription from JSON" }
                }
            }
    }
    .map(::Ready)
    .stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = Loading,
    )


private val json = Json {
    ignoreUnknownKeys = true
}