package hnau.ktiot.client.projector.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import hnau.common.mqtt.utils.Topic
import hnau.common.app.projector.uikit.HnauButton
import hnau.common.app.projector.uikit.TripleRow
import hnau.common.app.projector.uikit.table.Table
import hnau.common.app.projector.uikit.table.TableOrientation
import hnau.common.app.projector.utils.Icon
import hnau.ktiot.client.projector.property.PropertyProjector
import hnau.ktiot.client.projector.utils.toTitle
import hnau.ktiot.client.model.utils.ChildTopic
import kotlinx.collections.immutable.persistentListOf

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
                cells = remember(topic, onClick) {
                    persistentListOf(
                        {
                            HnauButton(
                                shape = shape,
                                onClick = onClick,
                            ) {
                                TripleRow(
                                    content = {
                                        Text(
                                            text = topic.toTitle(),
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                    },
                                    trailing = { Icon(Icons.Filled.ChevronRight) }
                                )
                            }
                        }
                    )
                }
            )
        }

        override val key: Int
            get() = 1
    }
}