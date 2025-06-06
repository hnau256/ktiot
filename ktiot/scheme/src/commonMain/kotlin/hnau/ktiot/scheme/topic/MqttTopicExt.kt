package hnau.ktiot.scheme.topic

import hnau.common.mqtt.utils.Topic


fun MqttTopic.asChild(
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

val MqttTopic.Absolute.ktiotElements: MqttTopic.Absolute
    get() = plus("ktiot")

val MqttTopic.Absolute.raw: Topic
    get() = Topic(MqttTopicParts.Companion.stringMapper.reverse(parts))
