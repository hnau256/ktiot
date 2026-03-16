package org.hnau.ktiot.mqtt

import org.hnau.ktiot.mqtt.platform.MqttClient
import org.hnau.ktiot.mqtt.platform.createMqttClient
import org.hnau.ktiot.mqtt.types.MqttConfig
import org.hnau.ktiot.mqtt.types.MqttState
import org.hnau.ktiot.mqtt.utils.MqttSessionImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.hnau.commons.kotlin.coroutines.flow.state.mutable.toMutableStateFlowAsInitial
import kotlin.math.pow
import kotlin.time.Clock
import kotlin.time.Instant

fun mqtt(
    scope: CoroutineScope,
    config: MqttConfig,
): StateFlow<MqttState> {
    val client: MqttClient = createMqttClient(
        config = config.broker,
    )

    val result: MutableStateFlow<MqttState> = MqttState.Connecting.toMutableStateFlowAsInitial()

    scope.launch {
        var attempt = 0
        while (true) {
            attempt++

            result.value = MqttState.Connecting

            val connectError = client.connect { simpleSession ->
                attempt = 0
                coroutineScope {
                    val sessionScope: CoroutineScope = this
                    val mqttSession = MqttSessionImpl(
                        scope = sessionScope,
                        simple = simpleSession,
                        unsubscribeDelay = config.unsubscribeDelay,
                    )
                    result.value = MqttState.Connected(
                        session = mqttSession,
                    )
                    awaitCancellation()
                }
            }

            if (attempt > 0) {
                val attemptDelay = with(config.reconnect) {
                    (initialDelay * multiplier.pow(attempt)).coerceAtMost(maxDelay)
                }

                val waitingForReconnectJob: Job = scope.launch { delay(attemptDelay) }
                result.value = MqttState.WaitingForReconnect(
                    disconnectedError = connectError,
                    nextAttemptAt = now() + attemptDelay,
                    connectNow = { waitingForReconnectJob.cancel() }
                )
                waitingForReconnectJob.join()
            }
        }
    }

    return result
}

private fun now(): Instant = Clock.System.now()