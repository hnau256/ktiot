package hnau.ktiot.coordinator.utils

import kotlin.time.Instant

data class Timestamped<out T>(
    val value: T,
    val timestamp: Instant,
)