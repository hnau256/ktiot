package hnau.ktiot.client.projector.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hnau.common.kotlin.coroutines.flatMapState
import hnau.common.kotlin.coroutines.flow.state.mapState
import hnau.common.kotlin.coroutines.flow.state.scopedInState
import hnau.common.kotlin.coroutines.flow.state.mutable.toMutableStateFlowAsInitial
import hnau.common.kotlin.fold
import hnau.common.kotlin.map
import hnau.common.projector.uikit.table.Cell
import hnau.common.projector.uikit.table.CellBox
import hnau.common.projector.uikit.table.Subtable
import hnau.common.projector.uikit.table.Table
import hnau.common.projector.uikit.table.TableOrientation
import hnau.common.projector.uikit.utils.Dimens
import hnau.common.projector.utils.Icon
import hnau.ktiot.client.model.property.PropertyModel
import hnau.ktiot.client.model.property.value.EditableModel
import hnau.ktiot.client.model.property.value.FlagModel
import hnau.ktiot.client.model.property.value.FractionModel
import hnau.ktiot.client.projector.property.value.EditableProjector
import hnau.ktiot.client.projector.property.value.FlagProjector
import hnau.ktiot.client.projector.property.value.FractionProjector
import hnau.ktiot.client.projector.utils.toTitle
import hnau.pipe.annotations.Pipe
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Immutable
class PropertyProjector(
    scope: CoroutineScope,
    private val model: PropertyModel,
    dependencies: Dependencies,
) {

    @Immutable
    @Pipe
    interface Dependencies {

        fun flag(): FlagProjector.Dependencies

        fun fraction(): FractionProjector.Dependencies

        fun editable(): EditableProjector.Dependencies
    }

    private val cells: StateFlow<ImmutableList<Cell>> = model
        .value
        .scopedInState(scope)
        .flatMapState(
            scope = scope,
        ) { (valueScope, valueOrErrorOrLoading) ->
            valueOrErrorOrLoading
                .map { valueOrError ->
                    valueOrError.map { value ->
                        when (value) {
                            is FractionModel -> FractionProjector(
                                scope = valueScope,
                                model = value,
                                dependencies = dependencies.fraction(),
                            )

                            is FlagModel -> FlagProjector(
                                scope = valueScope,
                                model = value,
                                dependencies = dependencies.flag(),
                            )

                            is EditableModel<*, *, *, *, *, *, *, *> -> EditableProjector(
                                scope = valueScope,
                                model = value,
                                dependencies = dependencies.editable(),
                            )
                        }
                    }
                }
                .fold(
                    ifLoading = {
                        buildCells(
                            scope = valueScope,
                            additionalTopCells = MutableStateFlow(
                                listOf {
                                    CellBox {
                                        CircularProgressIndicator(
                                            modifier = Modifier
                                                .padding(Dimens.smallSeparation)
                                                .size(32.dp)
                                        )
                                    }
                                }
                            ),
                        )
                    },
                    ifReady = { projectorOrError ->
                        projectorOrError.fold(
                            onFailure = { error ->
                                buildCells(
                                    scope = valueScope,
                                    additionalTopCells = MutableStateFlow(
                                        listOf {
                                            CellBox(
                                                backgroundColor = MaterialTheme.colorScheme.errorContainer,
                                            ) {
                                                Icon(
                                                    modifier = Modifier
                                                        .padding(Dimens.smallSeparation)
                                                        .size(32.dp),
                                                    icon = Icons.Filled.Error,
                                                )
                                            }
                                        }
                                    )
                                )
                            },
                            onSuccess = { projector ->
                                buildCells(
                                    scope = valueScope,
                                    additionalTopCells = projector.topCells,
                                    mainCells = projector.mainCells,
                                )
                            }
                        )
                    }
                )
        }

    private fun buildCells(
        scope: CoroutineScope,
        additionalTopCells: StateFlow<List<Cell>>,
        mainCells: StateFlow<List<Cell>> = emptyList<Cell>().toMutableStateFlowAsInitial(),
    ): StateFlow<ImmutableList<Cell>> {
        val topCells = additionalTopCells.mapState(scope) { currentAdditionalTopCells ->
            buildList<Cell> {
                add {
                    CellBox(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = Dimens.separation,
                                vertical = Dimens.smallSeparation,
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.smallSeparation),
                        ) {
                            Text(
                                text = model.topic.toTitle(),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
                addAll(currentAdditionalTopCells)
            }.toImmutableList()
        }
        return mainCells.mapState(scope) { currentMainCells ->
            buildList<Cell> {
                add {
                    Subtable(
                        cells = topCells.collectAsState().value,
                    )
                }
                addAll(currentMainCells.toImmutableList())
            }.toImmutableList()
        }
    }

    @Composable
    fun Content(
        modifier: Modifier,
    ) {
        Table(
            modifier = modifier,
            orientation = TableOrientation.Vertical,
            cells = cells.collectAsState().value,
        )
    }
}