package hnau.ktiot.coordinator.property

import hnau.ktiot.scheme.PropertyType
import hnau.ktiot.scheme.topic.MqttTopic

data class Property<T, P : PropertyType<T>, D : Property.Direction>(
    val topic: MqttTopic.Absolute,
    val type: P,
    val direction: D,
) {

    sealed interface Direction {

        data object Calculated : Direction

        data class In(
            val origin: InPropertyOrigin,
        ) : Direction
    }

    companion object
}