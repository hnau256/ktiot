package org.hnau.ktiot.mqtt.types.topic

import org.hnau.commons.kotlin.ifTrue


fun Topic.Absolute.tryRemovePrefix(
    base: Topic.Absolute,
): Topic.Relative? = parts
    .parts
    .tryRemovePrefix(
        prefix = base.parts.parts,
    )
    ?.let { tail ->
        Topic.Relative(
            parts = TopicParts(
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
