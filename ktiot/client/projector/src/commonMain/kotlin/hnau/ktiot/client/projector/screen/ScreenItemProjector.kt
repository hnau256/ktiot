package hnau.ktiot.client.projector.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import hnau.common.app.projector.uikit.ItemsRow
import hnau.common.app.projector.uikit.table.Table
import hnau.common.app.projector.uikit.table.TableOrientation
import hnau.common.app.projector.utils.Icon
import hnau.ktiot.client.model.utils.ChildTopic
import hnau.ktiot.client.projector.property.PropertyProjector
import hnau.ktiot.client.projector.utils.toTitle

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

    @Immutable
    data class ChildButton(
        val topic: ChildTopic,
        val onClick: () -> Unit,
    ) : ScreenItemProjector {

        @Composable
        override fun Content(
            modifier: Modifier,
        ) {
            Table(
                modifier = modifier,
                orientation = TableOrientation.Horizontal,
            ) {
                Cell { modifier ->
                    Button(
                        modifier = modifier,
                        shape = shape,
                        onClick = onClick,
                    ) {
                        ItemsRow {
                            Text(
                                text = topic.toTitle(),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Icon(Icons.Filled.ChevronRight)
                        }
                    }
                }
            }
        }

        override val key: Int
            get() = 1
    }
}