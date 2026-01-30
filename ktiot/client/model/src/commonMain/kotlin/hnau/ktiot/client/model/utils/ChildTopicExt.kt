package hnau.ktiot.client.model.utils

import hnau.ktiot.scheme.topic.MqttTopic


internal fun MqttTopic.asChild(
    parent: MqttTopic.Absolute,
): ChildTopic = when (this) {
    is MqttTopic.Absolute -> ChildTopic.Absolute(
        topic = this,
    )

    is MqttTopic.Relative -> ChildTopic.Relative(
        parent = parent,
        child = this,
    )
}