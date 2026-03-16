package org.hnau.ktiot.mqtt.types

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class ReconnectPolicy(
    val initialDelay: Duration = 1.seconds,
    val multiplier: Double = 2.0,
    val maxDelay: Duration = 60.seconds,
)