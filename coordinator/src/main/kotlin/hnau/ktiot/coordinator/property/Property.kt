package hnau.ktiot.coordinator.property

import hnau.common.mqtt.types.topic.Topic
import hnau.ktiot.scheme.PropertyType

data class Property<T, P : PropertyType<T>, D : Property.Direction>(
    val topic: Topic.Absolute,
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