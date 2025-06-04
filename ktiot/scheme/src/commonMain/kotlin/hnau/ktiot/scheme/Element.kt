package hnau.ktiot.scheme

import hnau.common.kotlin.mapper.Mapper
import hnau.ktiot.scheme.topic.MqttTopic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Serializable
sealed interface Element {

    val topic: MqttTopic

    /*@Serializable
    @SerialName("include")
    data class Include(
        override val topic: MqttTopic,
    ): Element

    @Serializable
    @SerialName("child")
    data class Child(
        override val topic: MqttTopic,
    ): Element*/

    @Serializable
    @SerialName("property")
    data class Property<T>(
        override val topic: MqttTopic,
        @SerialName("property_type")
        val type: PropertyType<T>,
        val mode: PropertyMode,
    ) : Element

    companion object {

        val listJsonMapper: Mapper<JsonElement, List<Element>> = ListSerializer(serializer())
            .let { serializer ->
                Mapper<JsonElement, List<Element>>(
                    direct = { encoded ->
                        Json.decodeFromJsonElement(
                            deserializer = serializer,
                            element = encoded,
                        )
                    },
                    reverse = { elements ->
                        Json.encodeToJsonElement(
                            serializer = serializer,
                            value = elements,
                        )
                    }
                )
            }
    }
}