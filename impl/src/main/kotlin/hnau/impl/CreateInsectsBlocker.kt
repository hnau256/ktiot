package hnau.impl

import hnau.common.kotlin.*
import hnau.common.kotlin.coroutines.flow.state.flatMapWithScope
import hnau.common.kotlin.coroutines.flow.state.mapState
import hnau.common.kotlin.coroutines.flow.state.mapWithScope
import hnau.common.kotlin.coroutines.flow.state.mutable.toMutableStateFlowAsInitial
import hnau.common.mqtt.utils.MqttClient
import hnau.ktiot.coordinator.asReadyStateFlow
import hnau.ktiot.coordinator.utils.ElementWithChildren
import hnau.ktiot.coordinator.property.*
import hnau.ktiot.scheme.topic.MqttTopic
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow


class InsectsBlocker(
    scope: CoroutineScope,
    topic: MqttTopic.Absolute,
    dependencies: Dependencies,
) {

    @Pipe
    interface Dependencies {

        val client: MqttClient

        fun manual(): InsectsBlockerManualConfig.Dependencies
    }

    private val manualProperty = topic
        .plus("manual")
        .flagProperty()
        .manual()

    private val manualConfigTopic = topic + "manualConfig"

    private val manual: StateFlow<Loadable<InsectsBlockerManualConfig?>> = manualProperty
        .subscribe(scope, dependencies.client) { false }
        .mapWithScope(scope) { scope, isManualOrLoading ->
            isManualOrLoading.map { isManual ->
                isManual.ifTrue {
                    InsectsBlockerManualConfig(
                        scope = scope,
                        dependencies = dependencies.manual(),
                        topic = manualConfigTopic,
                    )
                }
            }
        }

    private val isEnabled: StateFlow<Loadable<Boolean>> = manual.flatMapWithScope(scope) { scope, configOrLoading ->
        configOrLoading.fold(
            ifLoading = { Loading.toMutableStateFlowAsInitial() },
            ifReady = { configOrNull ->
                configOrNull.foldNullable(
                    ifNull = { false.asReadyStateFlow() /*TODO*/ },
                    ifNotNull = { config -> config.isEnabled }
                )
            }
        )
    }

    private val isEnabledProperty = topic
        .plus("enabled")
        .flagProperty()
        .calculated()

    init {
        isEnabledProperty.publish(
            scope = scope,
            client = dependencies.client,
            payload = isEnabled,
        )
    }

    val children: List<ElementWithChildren<*>> = listOf(
        manualProperty.element,
        ElementWithChildren(
            topic = manualConfigTopic,
            type = ElementWithChildren.Type.Child(
                included = true,
                children = manual.mapState(scope) { configOrLoading ->
                    configOrLoading.map { configOrNull ->
                        configOrNull
                            ?.children
                            ?: emptyList()
                    }
                }
            )
        ),
        isEnabledProperty.element,
    )
}

class InsectsBlockerManualConfig(
    scope: CoroutineScope,
    topic: MqttTopic.Absolute,
    dependencies: Dependencies,
) {

    @Pipe
    interface Dependencies {

        val client: MqttClient
    }

    private val isEnabledProperty = topic
        .plus("enabled")
        .flagProperty()
        .manual()

    val children: List<ElementWithChildren<*>> = listOf(
        isEnabledProperty.element
    )

    val isEnabled: StateFlow<Loadable<Boolean>> =
        isEnabledProperty.subscribe(scope, dependencies.client) { false }
}