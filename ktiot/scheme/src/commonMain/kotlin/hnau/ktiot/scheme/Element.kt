package hnau.ktiot.scheme

import hnau.ktiot.scheme.topic.MqttTopic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Element(
    val topic: MqttTopic,
    val title: String,
    val type: Type,
) {

    @Serializable
    sealed interface Type {

        @Serializable
        @SerialName("child")
        data class Child(
            val included: Boolean,
        ) : Type

        @Serializable
        @SerialName("property")
        data class Property<T>(
            @SerialName("property_type")
            val type: PropertyType<T>,
            val mode: PropertyMode,
        ) : Type
    }
}