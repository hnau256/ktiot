package hnau.ktiot.scheme

import hnau.common.kotlin.mapper.Mapper
import hnau.common.kotlin.mapper.bytesToString
import hnau.common.kotlin.mapper.plus
import hnau.ktiot.scheme.topic.MqttTopic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
data class Element(
    val topic: MqttTopic.Relative,
    val type: Type,
) {

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