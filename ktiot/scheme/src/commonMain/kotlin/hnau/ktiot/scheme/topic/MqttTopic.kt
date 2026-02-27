package hnau.ktiot.scheme.topic

import org.hnau.commons.kotlin.mapper.Mapper
import org.hnau.commons.kotlin.mapper.plus
import org.hnau.commons.kotlin.serialization.MappingKSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer

@Serializable(MqttTopic.Serializer::class)
sealed interface MqttTopic {

    @Serializable(Absolute.Serializer::class)
    data class Absolute(
        val parts: MqttTopicParts,
    ) : MqttTopic {

        constructor(
            vararg parts: String,
        ) : this(
            parts = MqttTopicParts(*parts),
        )

        operator fun plus(
            relative: Relative,
        ): Absolute = Absolute(
            parts = parts + relative.parts,
        )

        operator fun plus(
            part: String,
        ): Absolute = copy(
            parts = parts + part,
        )

        object Serializer : MappingKSerializer<String, Absolute>(
            base = String.serializer(),
            mapper = stringMapper,
        )

        companion object {

            val root = Absolute(
                parts = MqttTopicParts(
                    parts = emptyList(),
                )
            )

            val stringMapper: Mapper<String, Absolute> = Mapper<String, String>(
                direct = { withPrefix -> withPrefix.drop(1) },
                reverse = { withoutPrefix -> "${MqttTopicParts.Separator}$withoutPrefix" }
            ) + MqttTopicParts.stringMapper + Mapper(::Absolute, Absolute::parts)
        }
    }

    @Serializable(Relative.Serializer::class)
    data class Relative(
        val parts: MqttTopicParts,
    ) : MqttTopic {

        constructor(
            vararg parts: String,
        ) : this(
            parts = MqttTopicParts(*parts),
        )

        operator fun plus(
            relative: Relative,
        ): Relative = Relative(
            parts = parts + relative.parts,
        )

        operator fun plus(
            part: String,
        ): Relative = copy(
            parts = parts + part,
        )

        object Serializer : MappingKSerializer<String, Relative>(
            base = String.serializer(),
            mapper = stringMapper,
        )

        companion object {

            val stringMapper: Mapper<String, Relative> =
                MqttTopicParts.stringMapper + Mapper(::Relative, Relative::parts)
        }
    }

    object Serializer : MappingKSerializer<String, MqttTopic>(
        base = String.serializer(),
        mapper = stringMapper,
    )

    companion object {

        val stringMapper: Mapper<String, MqttTopic> = Mapper(
            direct = { string ->
                val (normalizedString, absolute) = when (string.startsWith(MqttTopicParts.Separator)) {
                    true -> string.drop(1) to true
                    false -> string to false
                }
                val parts = MqttTopicParts.stringMapper.direct(normalizedString)
                when (absolute) {
                    true -> Absolute(parts)
                    false -> Relative(parts)
                }
            },
            reverse = { topic ->
                val (parts, absolute) = when (topic) {
                    is Absolute -> topic.parts to true
                    is Relative -> topic.parts to false
                }
                val string = MqttTopicParts.stringMapper.reverse(parts)
                when (absolute) {
                    true -> "${MqttTopicParts.Separator}$string"
                    false -> string
                }
            },
        )
    }
}