package hnau.ktiot.coordinator.utils

import hnau.common.kotlin.coroutines.scoped
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch


internal fun buildScreen(
    topic: MqttTopic.Absolute,
    scope: CoroutineScope,
    client: MqttClient,
    builds: Flow<ScreenBuilder.() -> Unit>,
) {
    scope.launch {
        builds
            .scoped(scope)
            .collectLatest { (buildScope, build) ->
                val builder = ScreenBuilderImpl(
                    scope = buildScope,
                    topic = topic,
                    client = client,
                )
                builder.build()
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

    private fun <T> handleChildOrInclude(
        topic: MqttTopic,
        builds: Flow<ScreenBuilder.() -> T>,
    ): Flow<T> {
        val absoluteTopic = topic.asChild(this@ScreenBuilderImpl.topic).topic
        val builderWithResult = builds
            .scoped(scope)
            .map { (buildScope, build) ->
                val builder = ScreenBuilderImpl(
                    scope = buildScope,
                    topic = absoluteTopic,
                    client = client,
                )
                val result = builder.build()
                builder to result
            }
            .shareIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                replay = 1,
            )
        scope.launch {
            builderWithResult.collectLatest { (builder) ->
                client.publish(
                    topic = absoluteTopic.ktiotElements.raw,
                    retained = true,
                    payload = Element.listMqttPayloadMapper.reverse(builder.elements),
                )
            }
        }
        return builderWithResult.map { it.second }
    }

    override fun <T> child(
        topic: MqttTopic,
        builds: Flow<ScreenBuilder.() -> T>,
    ): Flow<T> {
        _elements.add(
            Element.Child(
                topic = topic,
            ),
        )
        return handleChildOrInclude(
            topic = topic,
            builds = builds,
        )
    }

    override fun <T> include(
        topic: MqttTopic,
        builds: Flow<ScreenBuilder.() -> T>,
    ): Flow<T> {
        _elements.add(
            Element.Include(
                topic = topic,
            ),
        )
        return handleChildOrInclude(
            topic = topic,
            builds = builds,
        )
    }

    override fun property(
        topic: MqttTopic,
    ): RawProperty = RawProperty(
        topic = topic.asChild(this@ScreenBuilderImpl.topic),
        client = client,
    )

    override fun <T, P : PropertyType<T>> share(
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