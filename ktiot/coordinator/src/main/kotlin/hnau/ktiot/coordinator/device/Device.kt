package hnau.ktiot.coordinator.device

import hnau.common.kotlin.Loadable
import kotlinx.coroutines.flow.StateFlow

data class Device(
    val name: String,
    val state: StateFlow<Loadable<State>>,
) {

    sealed interface State {

        data object Offline: State

        data class Online(
            val battery: Float?,
            val connectionQuality: Float,
        ): State
    }

}