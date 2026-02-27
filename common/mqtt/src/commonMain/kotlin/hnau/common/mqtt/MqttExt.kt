package org.hnau.commons.mqtt

import org.hnau.commons.kotlin.Mutable
import org.hnau.commons.kotlin.coroutines.createChild
import org.hnau.commons.mqtt.utils.MqttConfig
import org.hnau.commons.mqtt.utils.MqttState
import org.hnau.commons.mqtt.utils.createMqttClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

suspend fun mqtt(
    config: MqttConfig,
    block: suspend (MqttState) -> Unit,
) {
    coroutineScope {
        mqtt(
            scope = this,
            config = config,
        ).collectLatest { state ->
            block(state)
        }
    }
}

fun mqtt(
    scope: CoroutineScope,
    config: MqttConfig,
): StateFlow<MqttState> {
    val result: MutableStateFlow<MqttState> = MutableStateFlow(
        MqttState.Connecting,
    )
    scope.launch {
        val failedConnectionsCount: Mutable<Int> = Mutable(0)
        while (true) {
            loop(
                scope = scope.createChild(),
                config = config,
                failedConnectionsCount = failedConnectionsCount,
                onStateChanged = result::value::set
            )
        }
    }
    return result
}

private suspend inline fun loop(
    scope: CoroutineScope,
    config: MqttConfig,
    failedConnectionsCount: Mutable<Int>,
    onStateChanged: (MqttState) -> Unit,
) {
    onStateChanged(MqttState.Connecting)
    val (disconnectedCause, newFailedConnectionsCount) = createMqttClient(
        scope = scope,
        config = config,
    ).fold(
        onSuccess = { client ->
            onStateChanged(
                MqttState.Connected(
                    client = client
                )
            )
            try {
                scope
                    .async { awaitCancellation() }
                    .await()
            } catch (ex: CancellationException) {
                ex to 1
            }
        },
        onFailure = { error ->
            error to (failedConnectionsCount.value + 1)
        }
    )
    failedConnectionsCount.value = newFailedConnectionsCount
    val pauseBeforeReconnection = 5.seconds * (1 shl (newFailedConnectionsCount - 1))
    val delayJob = scope.launch { delay(pauseBeforeReconnection) }
    onStateChanged(
        MqttState.WaitingForReconnection(
            cause = disconnectedCause,
            reconnectionAt = Clock.System.now() + pauseBeforeReconnection,
            reconnectNow = { delayJob.cancel() }
        )
    )
    delayJob.join()
}