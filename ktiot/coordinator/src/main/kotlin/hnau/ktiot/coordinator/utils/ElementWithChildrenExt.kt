package hnau.ktiot.coordinator.utils

import hnau.common.kotlin.Loadable
import hnau.ktiot.scheme.Element
import hnau.ktiot.scheme.topic.MqttTopic
import hnau.ktiot.scheme.topic.tryRemovePrefix
import kotlinx.coroutines.flow.StateFlow

internal fun ElementWithChildren<*>.toElement(
    parent: MqttTopic.Absolute,
): Pair<Element, StateFlow<Loadable<List<ElementWithChildren<*>>>>?> {

    val (type, childrenOrNull) = when (type) {
        is ElementWithChildren.Type.Property<*> -> Element.Type.Property(
            type = type.type,
            mode = type.mode,
        ) to null

        is ElementWithChildren.Type.Child -> Element.Type.Child(
            included = type.included,
        ) to type.children
    }

    val element = Element(
        topic = topic
            .tryRemovePrefix(parent)
            ?: topic,
        type = type,
    )

    return element to childrenOrNull
}