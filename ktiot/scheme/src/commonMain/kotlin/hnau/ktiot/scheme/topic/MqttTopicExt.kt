package hnau.ktiot.scheme.topic

import hnau.common.mqtt.utils.Topic

fun MqttTopic.toAbsolute(
    relativePrefix: MqttTopic.Absolute,
): MqttTopic.Absolute = when (this) {
    is MqttTopic.Absolute -> this
    is MqttTopic.Relative -> relativePrefix + this
}

fun MqttTopic.Relative.toTitle(): String =
    parts.toTitle()

fun MqttTopic.Absolute.toTitle(): String =
    MqttTopicParts.Separator + parts.toTitle()

fun MqttTopic.toTitle(): String = when (this) {
    is MqttTopic.Absolute -> toTitle()
    is MqttTopic.Relative -> toTitle()
}

val MqttTopic.Absolute.ktiotElements: MqttTopic.Absolute
    get() = plus("ktiot")

val MqttTopic.Absolute.raw: Topic
    get() = Topic(MqttTopicParts.stringMapper.reverse(parts))
