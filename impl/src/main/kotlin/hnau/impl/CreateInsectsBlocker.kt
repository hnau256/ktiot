package hnau.impl

import hnau.ktiot.coordinator.ElementWithChildren
import hnau.ktiot.coordinator.client.RelativeMqttClient
import hnau.ktiot.coordinator.client.RelativeMqttClientImpl
import hnau.ktiot.coordinator.client.property
import hnau.ktiot.coordinator.utils.fallback
import hnau.ktiot.scheme.PropertyType
import kotlinx.coroutines.CoroutineScope

fun createInsectsBlocker(
    scope: CoroutineScope,
    client: RelativeMqttClient,
): List<ElementWithChildren<*>> {

    val isManual = client
        .property(
            topic = "manual",
            type = PropertyType.State.Flag,
        )
        .fallback(scope) { false }




}