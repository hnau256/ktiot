package org.hnau.ktiot.scheme

import org.hnau.ktiot.mqtt.types.topic.Topic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Element(
    val topic: Topic,
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