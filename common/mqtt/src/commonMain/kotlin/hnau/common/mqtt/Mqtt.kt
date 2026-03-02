package hnau.common.mqtt

import hnau.common.mqtt.internal.MqttActiveSessionAdapter
import hnau.common.mqtt.internal.MqttClient
import hnau.common.mqtt.internal.createMqttClient
import hnau.common.mqtt.utils.MqttConfig
import hnau.common.mqtt.utils.MqttConnectionStatus
import kotlinx.coroutines.Job
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
    onStatus: suspend (MqttConnectionStatus) -> Nothing,
): Nothing {
    val status = MutableStateFlow<MqttConnectionStatus>(MqttConnectionStatus.Connecting)
    val client = createMqttClient(config)
    coroutineScope {
        launch { status.collectLatest { onStatus(it) } }
        client.runReconnectLoop(config = config, onStatus = { status.value = it })
    }
}

private suspend fun MqttClient.runReconnectLoop(
    config: MqttConfig,
    onStatus: (MqttConnectionStatus) -> Unit,
): Nothing {
    var attempt = 0
    while (true) {
        onStatus(MqttConnectionStatus.Connecting)
        connect(config.broker) {
            attempt = 0
            onStatus(MqttConnectionStatus.Connected(MqttActiveSessionAdapter(this)))
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
) {
    val reconnectDelay = with(config.reconnect) {
        (initialDelay * multiplier.pow(attempt)).coerceAtMost(maxDelay)
    }
    var delayJob: Job? = null
    onStatus(
        MqttConnectionStatus.WaitingForReconnection(
            nextAttemptAt = Clock.System.now() + reconnectDelay,
            connectNow = { delayJob?.cancel() },
        ),
    )
    coroutineScope {
        delayJob = launch { delay(reconnectDelay) }
        delayJob.join()
    }
}
