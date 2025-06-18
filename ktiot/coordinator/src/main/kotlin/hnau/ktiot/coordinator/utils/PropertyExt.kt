package hnau.ktiot.coordinator.utils

import hnau.ktiot.scheme.PropertyType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

val PropertyFallbackDelay: Duration = 3.seconds

inline fun <T, P : PropertyType<T>> Property<T, P>.fallback(
    scope: CoroutineScope,
    delay: Duration = PropertyFallbackDelay,
    crossinline getValue: suspend () -> T,
): Property<T, P> = apply {
    scope.launch {
        try {
            withTimeout(
                timeout = delay,
            ) {
                subscribe().first()
            }
        } catch (_: TimeoutCancellationException) {
            val value = getValue()
            publish(
                payload = value,
                retained = true,
            )
        }
    }
}