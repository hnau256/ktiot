package hnau.common.mqtt

import hnau.common.mqtt.platform.MqttClient
import hnau.common.mqtt.platform.createMqttClient
import hnau.common.mqtt.types.MqttConfig
import hnau.common.mqtt.types.MqttState
import hnau.common.mqtt.utils.MqttSessionImpl
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.time.Clock
import kotlin.time.Instant

suspend fun mqtt(
    config: MqttConfig,
    block: suspend MqttState.() -> Unit,
) {
    val client: MqttClient = createMqttClient(
        config = config.broker,
    )

    coroutineScope {

        val scope: CoroutineScope = this

        var currentStateBlockJob: Job? = null

        val updateStateSyncObject = SynchronizedObject()
        val updateState: (MqttState) -> Unit = { state ->
            synchronized(updateStateSyncObject) {
                currentStateBlockJob?.cancel()
                currentStateBlockJob = scope.launch { state.block() }
            }
        }

        var attempt = 0
        while (true) {
            attempt++

            updateState(
                MqttState.Connecting(
                    started = now(),
                )
            )

            val connectError = client.connect { simpleSession ->
                attempt = 0
                coroutineScope {
                    val sessionScope: CoroutineScope = this
                    val mqttSession = MqttSessionImpl(
                        scope = sessionScope,
                        simple = simpleSession,
                        unsubscribeDelay = config.unsubscribeDelay,
                    )
                    updateState(
                        MqttState.Connected(
                            session = mqttSession,
                        )
                    )
                    awaitCancellation()
                }
            }

            if (attempt > 0) {
                val attemptDelay = with(config.reconnect) {
                    (initialDelay * multiplier.pow(attempt)).coerceAtMost(maxDelay)
                }

                val waitingForReconnectJob: Job = scope.launch { delay(attemptDelay) }
                updateState(
                    MqttState.WaitingForReconnect(
                        disconnectedError = connectError,
                        nextAttemptAt = now() + attemptDelay,
                        connectNow = { waitingForReconnectJob.cancel() }
                    )
                )
                waitingForReconnectJob.join()
            }
        }
    }
}

private fun now(): Instant = Clock.System.now()