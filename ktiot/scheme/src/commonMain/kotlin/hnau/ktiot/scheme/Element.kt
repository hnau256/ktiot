package hnau.ktiot.scheme

import hnau.ktiot.scheme.topic.MqttTopic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface Element {

    /*@Serializable
    @SerialName("include")
    data class Include(
        val topic: MqttTopic,
    ): Element

    @Serializable
    @SerialName("child")
    data class Child(
        val topic: MqttTopic,
    ): Element*/

    @Serializable
    @SerialName("property")
    data class Property<T>(
        val topic: MqttTopic,
        @SerialName("property_type")
        val type: PropertyType<T>,
        val mode: PropertyMode,
    ): Element
}