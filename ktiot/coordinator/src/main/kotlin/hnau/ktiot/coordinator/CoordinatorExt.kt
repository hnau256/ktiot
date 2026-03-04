package hnau.ktiot.coordinator

import co.touchlab.kermit.Logger
import hnau.common.mqtt.mqtt
import hnau.common.mqtt.types.MqttConfig
import hnau.common.mqtt.types.MqttSession
import hnau.common.mqtt.types.MqttState
import hnau.ktiot.coordinator.utils.ElementWithChildren
import hnau.ktiot.coordinator.utils.publishScheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import org.hnau.commons.kotlin.Loadable

private val logger = Logger.withTag("CoordinatorExt")

suspend fun coordinator(
    config: MqttConfig,
    createRootElements: (CoroutineScope, MqttSession) -> StateFlow<Loadable<List<ElementWithChildren<*>>>>,
) {
    coroutineScope {
        val scope: CoroutineScope = this
        mqtt(
            scope = scope,
            config = config,
        ).collectLatest { state ->
            logger.d { "State changed: $state" }
            when (state) {
                MqttState.Connecting, is MqttState.WaitingForReconnect -> Unit
                is MqttState.Connected -> coroutineScope {
                    val scope = this
                    val client = state.session
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
}