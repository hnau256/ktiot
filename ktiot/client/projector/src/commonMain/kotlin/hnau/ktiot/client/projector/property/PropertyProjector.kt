package hnau.ktiot.client.projector.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import hnau.common.kotlin.Loadable
import hnau.common.kotlin.coroutines.flatMapState
import hnau.common.kotlin.coroutines.mapState
import hnau.common.kotlin.coroutines.mapWithScope
import hnau.common.kotlin.coroutines.scopedInState
import hnau.common.kotlin.coroutines.toMutableStateFlowAsInitial
import hnau.common.kotlin.fold
import hnau.common.kotlin.ifNull
import hnau.common.kotlin.map
import hnau.common.kotlin.valueOrElse
import hnau.common.projector.uikit.ErrorPanel
import hnau.common.projector.uikit.progressindicator.ProgressIndicatorInBox
import hnau.common.projector.uikit.state.StateContent
import hnau.common.projector.uikit.state.TransitionSpec
import hnau.common.projector.uikit.table.Cell
import hnau.common.projector.uikit.table.CellBox
import hnau.common.projector.uikit.table.Subtable
import hnau.common.projector.uikit.table.Table
import hnau.common.projector.uikit.table.TableOrientation
import hnau.common.projector.uikit.utils.Dimens
import hnau.common.projector.utils.Icon
import hnau.ktiot.client.model.property.PropertyModel
import hnau.ktiot.client.model.property.value.EditableModel
import hnau.ktiot.client.model.property.value.FractionModel
import hnau.ktiot.client.projector.property.value.EditableProjector
import hnau.ktiot.client.projector.property.value.FractionProjector
import hnau.ktiot.client.projector.property.value.ValueProjector
import hnau.ktiot.client.projector.utils.icon
import hnau.ktiot.client.projector.utils.toTitle
import hnau.pipe.annotations.Pipe
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
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

        fun fraction(): FractionProjector.Dependencies

        fun editable(): EditableProjector.Dependencies
    }

    private val value: StateFlow<Loadable<Result<ValueProjector>>> = model.value.mapWithScope(
        scope = scope,
    ) { valueScope, valueOrErrorOrLoading ->
        valueOrErrorOrLoading.map { valueOrError ->
            valueOrError.map { value ->
                when (value) {
                    is FractionModel -> FractionProjector(
                        scope = valueScope,
                        model = value,
                        dependencies = dependencies.fraction(),
                    )

                    is EditableModel<*, *, *, *, *, *, *, *> -> EditableProjector(
                        scope = valueScope,
                        model = value,
                        dependencies = dependencies.editable(),
                    )
                }
            }
        }
    }

    private val topCells: StateFlow<ImmutableList<Cell>> = value
        .scopedInState(scope)
        .flatMapState(scope) { (valueScope, valueOrErrorOrLoading) ->
            valueOrErrorOrLoading
                .valueOrElse { null }
                ?.getOrNull()
                ?.topCells
                .ifNull { emptyList<Cell>().toMutableStateFlowAsInitial() }
                .mapState(valueScope) { actions ->
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
                                    Icon(
                                        icon = model.mode.icon,
                                    )
                                    Text(
                                        text = model.topic.toTitle(),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                            }
                        }
                        addAll(actions)
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
            cells = remember(topCells, value) {
                persistentListOf(
                    {
                        Subtable(
                            cells = topCells.collectAsState().value,
                        )
                    },
                    {
                        CellBox {
                            Box(
                                modifier = Modifier.padding(
                                    horizontal = Dimens.separation,
                                    vertical = Dimens.smallSeparation,
                                )
                            ) {
                                Value()
                            }
                        }
                    }
                )
            }
        )
    }

    @Composable
    private fun Value() {

        value
            .collectAsState()
            .value
            .StateContent(
                label = "propertyValueOrErrorOrLoading",
                transitionSpec = TransitionSpec.vertical(),
                contentKey = { valueOrErrorOrLoading: Loadable<Result<ValueProjector>> ->
                    valueOrErrorOrLoading.fold(
                        ifLoading = { 0 },
                        ifReady = { valueOrError ->
                            valueOrError.fold(
                                onFailure = { 1 },
                                onSuccess = { 2 },
                            )
                        }
                    )
                },
            ) { valueOrErrorOrLoading ->
                valueOrErrorOrLoading.fold(
                    ifLoading = { ProgressIndicatorInBox() },
                    ifReady = { valueOrError ->
                        valueOrError.fold(
                            onFailure = { error ->
                                ErrorPanel(
                                    title = {
                                        Text(
                                            text = error.message.toString(),
                                        )
                                    }
                                )
                            },
                            onSuccess = { valueProjector ->
                                valueProjector.MainContent()
                            },
                        )
                    }
                )
            }
    }
}