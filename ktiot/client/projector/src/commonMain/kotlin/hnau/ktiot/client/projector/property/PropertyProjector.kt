package hnau.ktiot.client.projector.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import hnau.common.projector.uikit.utils.Dimens
import hnau.common.projector.utils.Icon
import hnau.ktiot.client.model.property.PropertyModel
import hnau.ktiot.client.projector.utils.icon
import hnau.ktiot.client.projector.utils.toTitle
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope

@Immutable
class PropertyProjector(
    scope: CoroutineScope,
    private val model: PropertyModel,
    dependencies: Dependencies,
) {

    @Immutable
    @Pipe
    interface Dependencies {

    }

    @Composable
    fun Content(
        modifier: Modifier,
    ) {
        Card(
            modifier = modifier,
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = Dimens.separation,
                    vertical = Dimens.smallSeparation,
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.smallSeparation),
                ) {
                    Icon(
                        icon = model.mode.icon,
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = model.topic.toTitle(),
                        style = MaterialTheme.typography.titleMedium,
                    )

                }
            }
        }
    }
}