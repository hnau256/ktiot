package hnau.ktiot.coordinator

import hnau.common.mqtt.utils.MqttClient
import hnau.ktiot.scheme.PropertyMode
import hnau.ktiot.scheme.PropertyType
import hnau.ktiot.scheme.topic.MqttTopic
import hnau.ktiot.scheme.utils.PropertyAccessor

interface ScreenBuilder {

    val topic: MqttTopic.Absolute

    val client: MqttClient

    fun <T> property(
        topic: MqttTopic,
        type: PropertyType<T>,
        publishMode: PropertyMode?,
    ): PropertyAccessor<T>
}