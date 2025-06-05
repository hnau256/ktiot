package hnau.common.mqtt.utils

enum class QoS(
    val code: Int,
) {
    AtMostOnce(code = 0),
    AtLeastOnce(code = 1),
    ExactlyOnce(code = 2),
    ;

    companion object {

        val default = ExactlyOnce
    }
}
