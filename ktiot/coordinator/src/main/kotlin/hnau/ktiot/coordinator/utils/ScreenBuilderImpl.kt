package hnau.ktiot.coordinator.utils

import hnau.common.kotlin.coroutines.scopedInState
import hnau.common.mqtt.utils.MqttClient
import hnau.ktiot.coordinator.ScreenBuilder
import hnau.ktiot.scheme.Element
import hnau.ktiot.scheme.PropertyMode
import hnau.ktiot.scheme.PropertyType
import hnau.ktiot.scheme.topic.ChildTopic
import hnau.ktiot.scheme.topic.MqttTopic
import hnau.ktiot.scheme.topic.asChild
import hnau.ktiot.scheme.topic.ktiotElements
import hnau.ktiot.scheme.topic.raw
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
        builds
            .scopedInState(scope)
            .collectLatest { (buildScope, build) ->
            val builder = ScreenBuilderImpl(
                scope = buildScope,
                topic = topic,
                client = client,
            )
            builder.build(scope)
            client.publish(
                topic = topic.ktiotElements.raw,
                retained = true,
                payload = Element.listMqttPayloadMapper.reverse(builder.elements),
            )
        }
    }
}

private class ScreenBuilderImpl(
    override val scope: CoroutineScope,
    override val topic: MqttTopic.Absolute,
    override val client: MqttClient,
) : ScreenBuilder {

    private val _elements: MutableList<Element> = mutableListOf()

    val elements: List<Element>
        get() = _elements

    override fun property(
        topic: MqttTopic,
    ): RawProperty = RawProperty(
        topic = topic.asChild(this@ScreenBuilderImpl.topic),
        client = client,
    )

    override fun <T, P: PropertyType<T>> share(
        property: Property<T, P>,
        mode: PropertyMode,
    ) {
        _elements.add(
            Element.Property(
                topic = when (val topic = property.topic) {
                    is ChildTopic.Absolute -> topic.topic
                    is ChildTopic.Relative -> topic.child
                },
                type = property.type,
                mode = mode,
            ),
        )
    }
}