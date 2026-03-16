package org.hnau.ktiot.coordinator.utils

import org.hnau.ktiot.mqtt.types.topic.Topic
import org.hnau.ktiot.scheme.PropertyMode
import org.hnau.ktiot.scheme.PropertyType
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