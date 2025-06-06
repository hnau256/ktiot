package hnau.ktiot.client.projector.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import hnau.ktiot.client.projector.property.PropertyProjector

@Immutable
sealed interface ScreenItemProjector {

    @Composable
    fun Content(
        modifier: Modifier,
    )

    val key: Int

    @Immutable
    data class Property(
        private val projector: PropertyProjector,
    ) : ScreenItemProjector {

        @Composable
        override fun Content(
            modifier: Modifier,
        ) {
            projector.Content(
                modifier = modifier,
            )
        }

        override val key: Int
            get() = 0
    }
}