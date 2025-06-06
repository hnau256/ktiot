package hnau.ktiot.scheme.utils

import hnau.common.mqtt.utils.MqttClient
import hnau.ktiot.scheme.Element
import hnau.ktiot.scheme.topic.MqttTopic
import hnau.ktiot.scheme.topic.asChild

fun <T> Element.Property<T>.toAccessor(
    parentTopic: MqttTopic.Absolute,
    client: MqttClient,
): PropertyAccessor<T> = PropertyAccessor(
    topic = topic.asChild(parentTopic).topic,
    type = type,
    client = client,
)