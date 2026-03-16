package hnau.common.mqtt.types.topic

import org.hnau.commons.kotlin.mapper.Mapper
import org.hnau.commons.kotlin.mapper.plus
import org.hnau.commons.kotlin.mapper.stringSplit
import kotlin.jvm.JvmInline

@JvmInline
value class TopicParts(
    val parts: List<String>,
) {

    constructor(
        vararg parts: String,
    ) : this(
        parts = parts.toList(),
    )

    val encoded: String
        get() = stringMapper.reverse(this)

    override fun toString(): String = encoded

    operator fun plus(
        part: String,
    ): TopicParts = TopicParts(
        parts = parts + part,
    )

    operator fun plus(
        other: TopicParts,
    ): TopicParts = TopicParts(
        parts = parts + other.parts,
    )

    companion object {

        const val Separator = '/'

        val stringMapper: Mapper<String, TopicParts> = Mapper
            .stringSplit(separator = Separator)
            .plus(Mapper(::TopicParts, TopicParts::parts))
    }
}
