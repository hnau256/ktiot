package hnau.ktiot.coordinator.client

import hnau.ktiot.coordinator.ElementWithChildren
import hnau.ktiot.coordinator.property.Property
import hnau.ktiot.scheme.PropertyType
import hnau.ktiot.scheme.topic.MqttTopic

fun <T, P : PropertyType<T>> RelativeMqttClient.property(
    topic: String,
    type: P,
): Property<T, P> = property(
    topic = MqttTopic.Relative(topic),
    type = type,
)

fun <T : ElementWithChildren.Type> RelativeMqttClient.element(
    topic: String,
    block: (RelativeMqttClient) -> T,
): ElementWithChildren<T> = element(
    topic = MqttTopic.Relative(topic),
    block = block,
)