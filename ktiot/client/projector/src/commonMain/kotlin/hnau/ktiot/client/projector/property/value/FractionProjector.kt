package hnau.ktiot.client.projector.property.value

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import hnau.common.kotlin.foldBoolean
import hnau.common.app.projector.uikit.table.Cell
import hnau.common.app.projector.uikit.table.CellBox
import hnau.common.app.projector.uikit.utils.Dimens
import hnau.ktiot.client.model.property.value.FractionModel
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Immutable
class FractionProjector(
    scope: CoroutineScope,
    private val model: FractionModel,
    dependencies: Dependencies,
) : ValueProjector {

    @Immutable
    @Pipe
    interface Dependencies

    override val mainCells: StateFlow<List<Cell>> = MutableStateFlow(
        listOf {
            CellBox {
                val range = model.type.range
                val value = model
                    .value
                    .collectAsState()
                    .value
                model
                    .mutable
                    .foldBoolean(
                        ifTrue = {
                            Slider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = Dimens.separation,
                                        vertical = Dimens.smallSeparation,
                                    ),
                                value = value,
                                valueRange = model.type.range,
                                onValueChange = model::update,
                                onValueChangeFinished = model::publish,
                                enabled = model
                                    .isPublishing
                                    .collectAsState()
                                    .value
                                    .not(),
                            )
                        },
                        ifFalse = {
                            val normalizedValue = remember(value, range) {
                                range
                                    .takeIf { !it.isEmpty() }
                                    ?.let { nonEmptyRange ->
                                        (value - nonEmptyRange.start) /
                                                (nonEmptyRange.endInclusive - nonEmptyRange.start)
                                    }
                                    ?.fastCoerceIn(0f, 1f)
                                    ?: 0f
                            }
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Dimens.separation)
                                    .height(12.dp),
                                progress = { normalizedValue },
                            )
                        }
                    )
            }
        }
    )
}