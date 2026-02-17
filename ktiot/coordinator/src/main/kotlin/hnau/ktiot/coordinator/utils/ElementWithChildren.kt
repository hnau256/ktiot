package hnau.ktiot.coordinator.utils

import hnau.common.kotlin.Loadable
import hnau.ktiot.scheme.PropertyMode
import hnau.ktiot.scheme.PropertyType
import hnau.ktiot.scheme.topic.MqttTopic
import kotlinx.coroutines.flow.StateFlow

data class ElementWithChildren<out T: ElementWithChildren.Type>(
    val topic: MqttTopic.Absolute,
    val title: String,
    val type: T,
) {

    sealed interface Type {

        data class Child(
            val included: Boolean,
            val children: StateFlow<Loadable<List<ElementWithChildren<*>>>>,
        ) : Type

        data class Property<T>(
            val type: PropertyType<T>,
            val mode: PropertyMode,
        ) : Type
    }
}