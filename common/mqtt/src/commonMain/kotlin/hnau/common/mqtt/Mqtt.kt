package hnau.common.mqtt

import hnau.common.mqtt.internal.MqttActiveSessionAdapter
import hnau.common.mqtt.internal.MqttClient
import hnau.common.mqtt.internal.MultiplexedMqttSession
import hnau.common.mqtt.internal.createMqttClient
import hnau.common.mqtt.utils.MqttConfig
import hnau.common.mqtt.utils.MqttConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.time.Clock

suspend fun mqtt(
    config: MqttConfig,
    onStatus: suspend (MqttConnectionStatus) -> Unit,
) {
    val status = MutableStateFlow<MqttConnectionStatus>(MqttConnectionStatus.Connecting)
    val client = createMqttClient(config)
    coroutineScope {
        launch { status.collectLatest { onStatus(it) } }
        client.runReconnectLoop(config = config, scope = this, onStatus = { status.value = it })
    }
}

private suspend fun MqttClient.runReconnectLoop(
    config: MqttConfig,
    scope: CoroutineScope,
    onStatus: (MqttConnectionStatus) -> Unit,
): Nothing {
    var attempt = 0
    while (true) {
        onStatus(MqttConnectionStatus.Connecting)
        connect(config.broker) {
            attempt = 0
            val multiplexed =
                MultiplexedMqttSession(
                    session = this,
                    scope = scope,
                    subscriptionStopDelay = config.subscriptionStopDelay,
                )
            onStatus(MqttConnectionStatus.Connected(MqttActiveSessionAdapter(multiplexed)))
            awaitCancellation()
        }
        attempt++
        waitForReconnect(
            config = config,
            attempt = attempt,
            onStatus = onStatus,
        )
    }
}

private suspend fun waitForReconnect(
    config: MqttConfig,
    attempt: Int,
    onStatus: (MqttConnectionStatus) -> Unit,
) = coroutineScope {
    val reconnectDelay =
        with(config.reconnect) {
            (initialDelay * multiplier.pow(attempt)).coerceAtMost(maxDelay)
        }
    val delayJob = launch { delay(reconnectDelay) }
    onStatus(
        MqttConnectionStatus.WaitingForReconnection(
            nextAttemptAt = Clock.System.now() + reconnectDelay,
            connectNow = { delayJob.cancel() },
        ),
    )
    delayJob.join()
}
