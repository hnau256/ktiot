package hnau.common.mqtt.types

import kotlin.jvm.JvmInline

data class Message(
    val id: Id,
    val retained: Boolean,
    val payload: ByteArray,
    val qoS: QoS,
) {

    @JvmInline
    value class Id(
        val id: Int,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Message

        if (retained != other.retained) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = retained.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}