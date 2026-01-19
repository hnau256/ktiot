package hnau.ktiot.coordinator.property

import hnau.ktiot.scheme.PropertyType
import hnau.ktiot.scheme.topic.MqttTopic

class PropertyPrototype<T, P : PropertyType<T>>(
    val topic: MqttTopic.Absolute,
    val type: P,
)