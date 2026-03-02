package hnau.common.mqtt.utils

data class Message(
    val id: Int,
    val payload: ByteArray,
    val retained: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Message) return false
        return id == other.id &&
            payload.contentEquals(other.payload) &&
            retained == other.retained
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + retained.hashCode()
        return result
    }
}
