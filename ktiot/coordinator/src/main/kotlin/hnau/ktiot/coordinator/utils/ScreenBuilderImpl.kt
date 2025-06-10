package hnau.ktiot.coordinator.utils

import hnau.common.mqtt.utils.MqttClient
import hnau.ktiot.coordinator.ScreenBuilder
import hnau.ktiot.scheme.Element
import hnau.ktiot.scheme.PropertyMode
import hnau.ktiot.scheme.PropertyType
import hnau.ktiot.scheme.topic.MqttTopic
import hnau.ktiot.scheme.topic.asChild
import hnau.ktiot.scheme.topic.ktiotElements
import hnau.ktiot.scheme.topic.raw
import hnau.ktiot.scheme.utils.PropertyAccessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


internal fun buildScreen(
    topic: MqttTopic.Absolute,
    scope: CoroutineScope,
    client: MqttClient,
    builds: MutableStateFlow<ScreenBuilder.(CoroutineScope) -> Unit>,
) {
    scope.launch {
        builds.collectLatest { build ->
            val builder = ScreenBuilderImpl(
                topic = topic,
                scope = scope,
                client = client,
            )
            builder.build(scope)
            client.publish(
                topic = topic.ktiotElements.raw,
                retained = true,
                value = Element.listJsonMapper.reverse(builder.elements),
            )
        }
    }
}

private class ScreenBuilderImpl(
    private val scope: CoroutineScope,
    override val topic: MqttTopic.Absolute,
    override val client: MqttClient,
) : ScreenBuilder {

    private val _elements: MutableList<Element> = mutableListOf()

    val elements: List<Element>
        get() = _elements

    override fun <T> property(
        topic: MqttTopic,
        type: PropertyType<T>,
        publishMode: PropertyMode?,
    ): PropertyAccessor<T> {

        publishMode?.let { mode ->
            _elements.add(
                Element.Property(
                    topic = topic,
                    type = type,
                    mode = mode,
                ),
            )
        }

        return PropertyAccessor(
            type = type,
            client = client,
            topic = topic.asChild(this@ScreenBuilderImpl.topic).topic
        )
    }
}