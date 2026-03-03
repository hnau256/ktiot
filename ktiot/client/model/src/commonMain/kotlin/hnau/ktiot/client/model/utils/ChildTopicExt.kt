package hnau.ktiot.client.model.utils

import hnau.common.mqtt.types.topic.Topic


internal fun Topic.asChild(
    parent: Topic.Absolute,
): ChildTopic = when (this) {
    is Topic.Absolute -> ChildTopic.Absolute(
        topic = this,
    )

    is Topic.Relative -> ChildTopic.Relative(
        parent = parent,
        child = this,
    )
}