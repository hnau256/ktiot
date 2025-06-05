package hnau.ktiot.client.projector.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import hnau.ktiot.client.projector.property.PropertyProjector

@Immutable
sealed interface ScreenItemProjector {

    @Composable
    fun Content()

    val key: Int

    @Immutable
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