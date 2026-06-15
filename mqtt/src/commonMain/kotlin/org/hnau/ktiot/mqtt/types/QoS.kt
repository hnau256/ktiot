package org.hnau.ktiot.mqtt.types

import org.hnau.commons.gen.fold.annotations.Fold

@Fold
enum class QoS {
    AtMost,
    AtLeast,
    Exactly,;

    companion object {

        val default: QoS
            get() = AtLeast
    }
}