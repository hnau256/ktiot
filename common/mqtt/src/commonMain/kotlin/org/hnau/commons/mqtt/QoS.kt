package org.hnau.commons.mqtt

enum class QoS {
    AtMostOnce,
    AtLeastOnce,
    ExactlyOnce,
    ;

    internal val mqttCode: Int get() = ordinal

    companion object {
        val Default = ExactlyOnce
    }
}
