package org.hnau.ktiot.coordinator.utils

import org.hnau.ktiot.mqtt.types.topic.Topic
import org.hnau.ktiot.mqtt.types.topic.tryRemovePrefix
import org.hnau.ktiot.scheme.Element
import kotlinx.coroutines.flow.StateFlow
import org.hnau.commons.kotlin.Loadable

internal fun ElementWithChildren<*>.toElement(
    parent: Topic.Absolute,
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
        title = title,
        type = type,
    )

    return element to childrenOrNull
}