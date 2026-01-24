package hnau.impl

import hnau.common.mqtt.utils.MqttClient
import hnau.ktiot.coordinator.asReadyStateFlow
import hnau.ktiot.coordinator.utils.ElementWithChildren
import hnau.ktiot.scheme.topic.MqttTopic
import kotlinx.coroutines.CoroutineScope

fun createHome(
    scope: CoroutineScope,
    topic: MqttTopic.Absolute,
    client: MqttClient,
): List<ElementWithChildren<*>> = (0..1).map { index ->

    val childTopic = topic + "room_${index + 1}"

    val blocker = InsectsBlocker(
        scope = scope,
        topic = childTopic,
        client = client,
    )

    ElementWithChildren(
        topic = childTopic,
        type = ElementWithChildren.Type.Child(
            included = false,
            children = blocker
                .children
                .asReadyStateFlow(),
        )
    )
}