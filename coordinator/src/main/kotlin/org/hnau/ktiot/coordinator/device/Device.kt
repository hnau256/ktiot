package org.hnau.ktiot.coordinator.device

import kotlinx.coroutines.flow.StateFlow
import org.hnau.commons.kotlin.Loadable

data class Device(
    val name: String,
    val state: StateFlow<Loadable<State>>,
) {

    sealed interface State {

        data object Offline: State

        data class Online(
            val battery: Float?,
            val connectionQuality: Float?,
        ): State
    }

}