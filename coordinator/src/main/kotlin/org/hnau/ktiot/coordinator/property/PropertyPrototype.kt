package org.hnau.ktiot.coordinator.property

import org.hnau.ktiot.mqtt.types.topic.Topic
import org.hnau.ktiot.scheme.PropertyType

class PropertyPrototype<T, P : PropertyType<T>>(
    val topic: Topic.Absolute,
    val type: P,
)