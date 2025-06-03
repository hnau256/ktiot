package hnau.ktiot.scheme.topic

import hnau.common.kotlin.mapper.Mapper
import hnau.common.kotlin.mapper.plus
import hnau.common.kotlin.mapper.stringSplit
import kotlin.jvm.JvmInline

@JvmInline
value class MqttTopicParts(
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
    ): MqttTopicParts = MqttTopicParts(
        parts = parts + part,
    )

    operator fun plus(
        other: MqttTopicParts,
    ): MqttTopicParts = MqttTopicParts(
        parts = parts + other.parts,
    )

    companion object {

        const val Separator = '/'

        val stringMapper: Mapper<String, MqttTopicParts> = Mapper
            .stringSplit(separator = Separator)
            .plus(Mapper(::MqttTopicParts, MqttTopicParts::parts))
    }
}
