package hnau.common.logging

import io.github.oshai.kotlinlogging.KLogger
import kotlinx.coroutines.CancellationException

inline fun <R> KLogger.tryOrLog(
    log: String,
    block: () -> R,
): Result<R> = try {
    val result = block()
    Result.success(result)
} catch (ex: CancellationException) {
    throw ex
} catch (th: Throwable) {
    warn(th) { "Error while $log" }
    Result.failure(th)
}