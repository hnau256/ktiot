package hnau.ktiot.coordinator.property

import hnau.common.mqtt.utils.MqttClient
import hnau.ktiot.scheme.PropertyType
import hnau.ktiot.scheme.topic.MqttTopic

fun <T, P : PropertyType<T>> MqttClient.property(
    topic: MqttTopic.Absolute,
    type: P,
): Property<T, P> = PropertyImpl(
    client = this,
    topic = topic,
    type = type,
)