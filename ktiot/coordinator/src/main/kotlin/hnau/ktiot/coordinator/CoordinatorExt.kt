package hnau.ktiot.coordinator

import hnau.common.mqtt.mqtt
import hnau.common.mqtt.utils.MqttConfig
import hnau.common.mqtt.utils.MqttState
import hnau.ktiot.coordinator.utils.buildScreen
import hnau.ktiot.scheme.topic.MqttTopic
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private val logger: KLogger = KotlinLogging.logger { }

suspend fun coordinator(
    config: MqttConfig,
    builds: StateFlow<ScreenBuilder.() -> Unit>,
) {
    mqtt(
        config = config,
    ) { state ->
        logger.debug { "State changed: $state" }
        when (state) {
            MqttState.Connecting, is MqttState.WaitingForReconnection -> Unit
            is MqttState.Connected -> coroutineScope {
                val scope = this
                buildScreen(
                    topic = MqttTopic.Absolute.root,
                    scope = scope,
                    client = state.client,
                    builds = builds,
                )
                awaitCancellation()
            }
        }
    }
}