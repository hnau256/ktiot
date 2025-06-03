package hnau.common.logging

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import io.github.oshai.kotlinlogging.KLogger
import kotlinx.coroutines.CancellationException

inline fun <R> KLogger.tryOrLog(
    log: String,
    block: () -> R,
): Option<R> = try {
    block().let(::Some)
} catch (ex: CancellationException) {
    throw ex
} catch (th: Throwable) {
    warn(th) { "Error while $log" }
    None
}