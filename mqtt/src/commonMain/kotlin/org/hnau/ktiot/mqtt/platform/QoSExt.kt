package org.hnau.ktiot.mqtt.platform

import org.hnau.ktiot.mqtt.types.QoS
import org.hnau.commons.kotlin.mapper.Mapper
import org.hnau.commons.kotlin.mapper.toEnum

private val qosIdMapper: Mapper<Int, QoS> = Mapper.toEnum<Int, QoS>(
    extractValue = {
        when (this) {
            QoS.AtMost -> 0
            QoS.AtLeast -> 1
            QoS.Exactly -> 2
        }
    }
)

internal val QoS.Companion.idMapper: Mapper<Int, QoS>
    get() = qosIdMapper