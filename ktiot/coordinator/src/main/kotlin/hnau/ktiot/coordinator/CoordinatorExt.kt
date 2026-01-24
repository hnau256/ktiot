package hnau.ktiot.coordinator

import hnau.common.kotlin.Loadable
import hnau.common.mqtt.mqtt
import hnau.common.mqtt.utils.MqttClient
import hnau.common.mqtt.utils.MqttConfig
import hnau.common.mqtt.utils.MqttState
import hnau.ktiot.coordinator.utils.ElementWithChildren
import hnau.ktiot.coordinator.utils.publishScheme
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow

private val logger: KLogger = KotlinLogging.logger { }

suspend fun coordinator(
    config: MqttConfig,
    createRootElements: (CoroutineScope, MqttClient) -> StateFlow<Loadable<List<ElementWithChildren<*>>>>,
) {
    mqtt(
        config = config,
    ) { state ->
        logger.debug { "State changed: $state" }
        when (state) {
            MqttState.Connecting, is MqttState.WaitingForReconnection -> Unit
            is MqttState.Connected -> coroutineScope {
                val scope = this
                val client = state.client
                val rootElements = createRootElements(scope, client)
                client.publishScheme(
                    scope = scope,
                    rootElements = rootElements,
                )
                awaitCancellation()
            }
        }
    }
}