package hnau.ktiot.coordinator.property

import hnau.common.mqtt.types.topic.Topic
import hnau.ktiot.scheme.PropertyType

class PropertyPrototype<T, P : PropertyType<T>>(
    val topic: Topic.Absolute,
    val type: P,
)