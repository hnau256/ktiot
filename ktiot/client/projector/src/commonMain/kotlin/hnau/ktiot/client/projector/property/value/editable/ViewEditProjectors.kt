package hnau.ktiot.client.projector.property.value.editable

import androidx.compose.runtime.Composable

sealed interface ViewProjector {

    @Composable
    fun Content()
}

sealed interface EditProjector {

    @Composable
    fun Content()
}