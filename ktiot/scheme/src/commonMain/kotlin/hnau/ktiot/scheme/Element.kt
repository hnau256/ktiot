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
sealed interface Element {

    val topic: MqttTopic

    @Serializable
    @SerialName("child")
    data class Child(
        override val topic: MqttTopic,
    ): Element

    @Serializable
    @SerialName("include")
    data class Include(
        override val topic: MqttTopic,
    ): Element

    @Serializable
    @SerialName("property")
    data class Property<T>(
        override val topic: MqttTopic,
        @SerialName("property_type")
        val type: PropertyType<T>,
        val mode: PropertyMode,
    ) : Element

    companion object {

        val listMqttPayloadMapper: Mapper<ByteArray, List<Element>> = ListSerializer(serializer())
            .let { serializer ->
                Mapper.bytesToString + Mapper<String, List<Element>>(
                    direct = { json ->
                        Json.decodeFromString(
                            deserializer = serializer,
                            string = json,
                        )
                    },
                    reverse = { elements ->
                        Json.encodeToString(
                            serializer = serializer,
                            value = elements,
                        )
                    }
                )
            }
    }
}