package hnau.ktiot.client.model.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class Timestamped<out T>(
    val timestamp: Instant,
    val value: T,
) {

    companion object {

        fun <T> now(
            value: T,
        ): Timestamped<T> = Timestamped(
            value = value,
            timestamp = Clock.System.now(),
        )
    }
}