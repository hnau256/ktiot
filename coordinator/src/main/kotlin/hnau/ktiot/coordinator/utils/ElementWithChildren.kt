package hnau.ktiot.coordinator.utils

import hnau.common.mqtt.types.topic.Topic
import hnau.ktiot.scheme.PropertyMode
import hnau.ktiot.scheme.PropertyType
import kotlinx.coroutines.flow.StateFlow
import org.hnau.commons.kotlin.Loadable

data class ElementWithChildren<out T: ElementWithChildren.Type>(
    val topic: Topic.Absolute,
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