package hnau.ktiot.scheme.topic

import hnau.common.kotlin.ifTrue
import hnau.common.mqtt.utils.Topic
import hnau.ktiot.scheme.SchemeConstants

val MqttTopic.Absolute.ktiotElements: MqttTopic.Absolute
    get() = plus(SchemeConstants.schemeTopic)

val MqttTopic.Absolute.raw: Topic
    get() = Topic(MqttTopicParts.Companion.stringMapper.reverse(parts))


fun MqttTopic.Absolute.tryRemovePrefix(
    base: MqttTopic.Absolute,
): MqttTopic.Relative? = parts
    .parts
    .tryRemovePrefix(
        prefix = base.parts.parts,
    )
    ?.let { tail ->
        MqttTopic.Relative(
            parts = MqttTopicParts(
                parts = tail.toList(),
            )
        )
    }

private fun <T> Iterable<T>.tryRemovePrefix(
    prefix: Iterable<T>,
): Sequence<T>? {
    val iterator = iterator()
    return prefix
        .all { prefixElement -> iterator.hasNext() && iterator.next() == prefixElement }
        .ifTrue { iterator.asSequence() }
}
