package hnau.ktiot.coordinator.property

import hnau.ktiot.scheme.PropertyType
import hnau.common.mqtt.types.topic.Topic

class PropertyPrototype<T, P : PropertyType<T>>(
    val topic: Topic.Absolute,
    val type: P,
)