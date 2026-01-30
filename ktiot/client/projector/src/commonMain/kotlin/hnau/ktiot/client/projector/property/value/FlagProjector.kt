package hnau.ktiot.client.projector.property.value

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import hnau.common.kotlin.foldBoolean
import hnau.common.app.projector.uikit.table.Cell
import hnau.common.app.projector.uikit.table.CellBox
import hnau.common.app.projector.uikit.utils.Dimens
import hnau.ktiot.client.model.property.value.FlagModel
import hnau.ktiot.client.projector.Res
import hnau.ktiot.client.projector.no
import hnau.ktiot.client.projector.yes
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource

@Immutable
class FlagProjector(
    scope: CoroutineScope,
    private val model: FlagModel,
    dependencies: Dependencies,
) : ValueProjector {

    @Immutable
    @Pipe
    interface Dependencies

    override val topCells: StateFlow<List<Cell>> = MutableStateFlow(
        listOf {
            CellBox {
                val value = model
                    .value
                    .collectAsState()
                    .value
                model
                    .mutable
                    .foldBoolean(
                        ifFalse = {
                            Text(
                                modifier = Modifier.padding(
                                    horizontal = Dimens.separation,
                                    vertical = Dimens.smallSeparation,
                                ),
                                text = stringResource(
                                    value.foldBoolean(
                                        ifTrue = { Res.string.yes },
                                        ifFalse = { Res.string.no }
                                    )
                                ),
                                color = value.foldBoolean(
                                    ifTrue = { MaterialTheme.colorScheme.primary },
                                    ifFalse = { MaterialTheme.colorScheme.error }
                                ),
                                style = MaterialTheme.typography.titleLarge,
                            )
                        },
                        ifTrue = {
                            Switch(
                                modifier = Modifier.padding(
                                    horizontal = Dimens.separation,
                                    vertical = Dimens.smallSeparation,
                                ),
                                checked = value,
                                onCheckedChange = { model.publish.value?.invoke(it) },
                                enabled = model.publish.collectAsState().value != null,
                            )
                        }
                    )
            }
        }
    )
}