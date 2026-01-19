package hnau.ktiot.coordinator.property

import hnau.common.kotlin.Loadable
import hnau.common.kotlin.Loading
import hnau.common.kotlin.Ready
import hnau.common.kotlin.coroutines.toMutableStateFlowAsInitial
import hnau.ktiot.coordinator.ElementWithChildren
import hnau.ktiot.scheme.PropertyMode
import hnau.ktiot.scheme.PropertyType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

inline fun <T, P : PropertyType<T>> Property<T, P>.subscribeToState(
    scope: CoroutineScope,
    fallbackDelay: Duration = PropertyFallbackDelay,
    crossinline getFallbackValue: suspend () -> T,
): StateFlow<Loadable<T>> {
    val result: MutableStateFlow<Loadable<T>> = Loading.toMutableStateFlowAsInitial()
    scope.launch {
        subscribe()
            .map(::Ready)
            .collect(result)
    }
    scope.launch {
        try {
            withTimeout(
                timeout = fallbackDelay,
            ) {
                result.first {valueOrLoading ->
                    when (valueOrLoading) {
                        Loading -> false
                        is Ready -> true
                    }
                }
            }
        } catch (_: TimeoutCancellationException) {
            val fallbackValue = getFallbackValue()
            publish(
                payload = fallbackValue,
                retained = true,
            )
        }
    }
    return result
}


fun <T, P : PropertyType<T>> Property<T, P>.toElement(
    mode: PropertyMode,
): ElementWithChildren<ElementWithChildren.Type.Property<T>> = ElementWithChildren(
    topic = topic,
    type = ElementWithChildren.Type.Property(
        type = type,
        mode = mode,
    ),
)