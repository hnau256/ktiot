package hnau.common.mqtt.utils

data class Message(
    val id: Int,
    val payload: ByteArray,
    val retained: Boolean,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Message

        if (id != other.id) return false
        if (retained != other.retained) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + retained.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}