package hnau.common.mqtt.types

enum class QoS {
    AtMost,
    AtLeast,
    Exactly,;

    companion object {

        val default: QoS
            get() = AtLeast
    }
}