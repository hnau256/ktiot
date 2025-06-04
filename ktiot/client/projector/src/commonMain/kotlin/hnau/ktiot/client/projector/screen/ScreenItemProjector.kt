package hnau.ktiot.client.projector.screen

import androidx.compose.runtime.Composable
import hnau.ktiot.client.projector.property.PropertyProjector

sealed interface ScreenItemProjector {

    @Composable
    fun Content()

    val key: Int

    data class Property(
        private val projector: PropertyProjector,
    ) : ScreenItemProjector {

        @Composable
        override fun Content() {
            projector.Content()
        }

        override val key: Int
            get() = 0
    }
}