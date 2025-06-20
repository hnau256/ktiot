package hnau.ktiot.coordinator

import hnau.common.mqtt.utils.MqttClient
import hnau.ktiot.coordinator.utils.Property
import hnau.ktiot.coordinator.utils.PropertyFallbackDelay
import hnau.ktiot.coordinator.utils.RawProperty
import hnau.ktiot.coordinator.utils.fallback
import hnau.ktiot.scheme.PropertyMode
import hnau.ktiot.scheme.PropertyType
import hnau.ktiot.scheme.topic.MqttTopic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration

interface ScreenBuilder {

    val scope: CoroutineScope

    val topic: MqttTopic.Absolute

    val client: MqttClient

    fun <T> child(
        topic: MqttTopic,
        builds: Flow<ScreenBuilder.() -> T>,
    ): Flow<T>

    fun <T> child(
        topicPart: String,
        builds: Flow<ScreenBuilder.() -> T>,
    ): Flow<T> = child(
        topic = MqttTopic.Relative(topicPart),
        builds = builds,
    )

    fun <T> include(
        topic: MqttTopic,
        builds: Flow<ScreenBuilder.() -> T>,
    ): Flow<T>

    fun <T> include(
        topicPart: String,
        builds: Flow<ScreenBuilder.() -> T>,
    ): Flow<T> = include(
        topic = MqttTopic.Relative(topicPart),
        builds = builds,
    )

    fun property(
        topic: MqttTopic,
    ): RawProperty

    fun property(
        topicPart: String,
    ): RawProperty = property(
        topic = MqttTopic.Relative(topicPart),
    )

    fun <T, P: PropertyType<T>> share(
        property: Property<T, P>,
        mode: PropertyMode,
    )

    fun <T, P: PropertyType<T>> Property<T, P>.share(
        mode: PropertyMode,
    ): Property<T, P> = apply {
        share(
            property = this,
            mode = mode,
        )
    }

    fun <T, P: PropertyType<T>> Property<T, P>.fallback(
        delay: Duration = PropertyFallbackDelay,
        getValue: suspend () -> T,
    ): Property<T, P> = apply {
        fallback(
            scope = scope,
            delay = delay,
            getValue = getValue,
        )
    }

    fun <T, P: PropertyType<T>> Property<T, P>.bind(
        values: Flow<T>,
        retained: Boolean,
    ) {
        scope.launch {
            values.collectLatest { value ->
                publish(
                    payload = value,
                    retained = retained,
                )
            }
        }
    }
}