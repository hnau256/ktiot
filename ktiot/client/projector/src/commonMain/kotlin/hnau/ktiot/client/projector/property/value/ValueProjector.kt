package hnau.ktiot.client.projector.property.value

import androidx.compose.runtime.Composable

sealed interface ValueProjector {

    @Composable
    fun TopContent() {}

    @Composable
    fun MainContent()
}