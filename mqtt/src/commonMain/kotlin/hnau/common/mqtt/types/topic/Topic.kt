package hnau.common.mqtt.types.topic

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import org.hnau.commons.kotlin.mapper.Mapper
import org.hnau.commons.kotlin.mapper.plus
import org.hnau.commons.kotlin.serialization.MappingKSerializer

@Serializable(Topic.Serializer::class)
sealed interface Topic {

    @Serializable(Absolute.Serializer::class)
    data class Absolute(
        val parts: TopicParts,
    ) : Topic {

        constructor(
            vararg parts: String,
        ) : this(
            parts = TopicParts(*parts),
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

        override fun toString(): String =
            stringMapper.reverse(this)

        object Serializer : MappingKSerializer<String, Absolute>(
            base = String.serializer(),
            mapper = stringMapper,
        )

        companion object {

            val root = Absolute(
                parts = TopicParts(
                    parts = emptyList(),
                )
            )

            val stringMapper: Mapper<String, Absolute> = Mapper<String, String>(
                direct = { withPrefix -> withPrefix.drop(1) },
                reverse = { withoutPrefix -> "${TopicParts.Separator}$withoutPrefix" }
            ) + TopicParts.stringMapper + Mapper(::Absolute, Absolute::parts)
        }
    }

    @Serializable(Relative.Serializer::class)
    data class Relative(
        val parts: TopicParts,
    ) : Topic {

        constructor(
            vararg parts: String,
        ) : this(
            parts = TopicParts(*parts),
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

        override fun toString(): String =
            stringMapper.reverse(this)

        object Serializer : MappingKSerializer<String, Relative>(
            base = String.serializer(),
            mapper = stringMapper,
        )

        companion object {

            val stringMapper: Mapper<String, Relative> =
                TopicParts.stringMapper + Mapper(::Relative, Relative::parts)
        }
    }

    object Serializer : MappingKSerializer<String, Topic>(
        base = String.serializer(),
        mapper = stringMapper,
    )

    companion object {

        val stringMapper: Mapper<String, Topic> = Mapper(
            direct = { string ->
                val (normalizedString, absolute) = when (string.startsWith(TopicParts.Separator)) {
                    true -> string.drop(1) to true
                    false -> string to false
                }
                val parts = TopicParts.stringMapper.direct(normalizedString)
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
                val string = TopicParts.stringMapper.reverse(parts)
                when (absolute) {
                    true -> "${TopicParts.Separator}$string"
                    false -> string
                }
            },
        )
    }
}