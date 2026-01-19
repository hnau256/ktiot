package hnau.impl

import hnau.common.kotlin.*
import hnau.common.kotlin.coroutines.flatMapWithScope
import hnau.common.kotlin.coroutines.mapState
import hnau.common.kotlin.coroutines.mapWithScope
import hnau.common.kotlin.coroutines.toMutableStateFlowAsInitial
import hnau.common.mqtt.utils.MqttClient
import hnau.ktiot.coordinator.ElementWithChildren
import hnau.ktiot.coordinator.property.Property
import hnau.ktiot.coordinator.property.property
import hnau.ktiot.coordinator.property.subscribeToState
import hnau.ktiot.coordinator.property.toElement
import hnau.ktiot.scheme.PropertyMode
import hnau.ktiot.scheme.PropertyType
import hnau.ktiot.scheme.topic.MqttTopic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class InsectsBlocker(
    scope: CoroutineScope,
    topic: MqttTopic.Absolute,
    client: MqttClient,
) {

    private val isManualProperty: Property<Boolean, PropertyType.State.Flag> = client
        .property(
            topic = topic + "manual",
            type = PropertyType.State.Flag,
        )

    private val manualConfigTopic = topic + "manualConfig"

    private val manual: StateFlow<Loadable<InsectsBlockerManualConfig?>> = isManualProperty
        .subscribeToState(scope) { false }
        .mapWithScope(scope) { scope, isManualOrLoading ->
            isManualOrLoading.map { isManual ->
                isManual.ifTrue {
                    InsectsBlockerManualConfig(
                        scope = scope,
                        client = client,
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
                    ifNull = { TODO("Calculate") },
                    ifNotNull = { config -> config.isEnabled }
                )
            }
        )
    }

    private val isEnabledProperty = client
        .property(
            topic = topic + "enabled",
            type = PropertyType.State.Flag,
        )
        .apply {
            scope.launch {
                isEnabled.collect { enabledOrLoading ->
                    enabledOrLoading.fold(
                        ifLoading = {},
                        ifReady = { enabled ->
                            publish(
                                payload = enabled,
                                retained = true,
                            )
                        }
                    )
                }
            }
        }

    val children: List<ElementWithChildren<*>> = listOf(
        isManualProperty.toElement(
            mode = PropertyMode.Manual,
        ),
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
        isEnabledProperty.toElement(
            mode = PropertyMode.Calculated,
        ),
    )
}

class InsectsBlockerManualConfig(
    scope: CoroutineScope,
    topic: MqttTopic.Absolute,
    client: MqttClient,
) {

    private val isEnabledProperty: Property<Boolean, PropertyType.State.Flag> = client
        .property(
            topic = topic + "enabled",
            type = PropertyType.State.Flag,
        )

    val children: List<ElementWithChildren<*>> = listOf(
        isEnabledProperty.toElement(
            mode = PropertyMode.Manual,
        )
    )

    val isEnabled: StateFlow<Loadable<Boolean>> =
        isEnabledProperty.subscribeToState(scope) { false }
}