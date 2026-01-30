package hnau.ktiot.client.projector.property.value

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import hnau.common.kotlin.coroutines.flow.state.flatMapState
import hnau.common.kotlin.coroutines.flow.state.mapState
import hnau.common.kotlin.coroutines.flow.state.mapWithScope
import hnau.common.kotlin.coroutines.flow.state.scopedInState
import hnau.common.app.projector.uikit.HnauButton
import hnau.common.app.projector.uikit.table.Cell
import hnau.common.app.projector.utils.Icon
import hnau.ktiot.client.model.property.value.EditableModel
import hnau.ktiot.client.model.property.value.editable.EditModel
import hnau.ktiot.client.model.property.value.editable.TextEditModel
import hnau.ktiot.client.model.property.value.editable.TextViewModel
import hnau.ktiot.client.model.property.value.editable.ViewModel
import hnau.ktiot.client.projector.property.value.editable.EditProjector
import hnau.ktiot.client.projector.property.value.editable.TextEditProjector
import hnau.ktiot.client.projector.property.value.editable.TextViewProjector
import hnau.ktiot.client.projector.property.value.editable.ViewProjector
import hnau.ktiot.scheme.PropertyType
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Immutable
class EditableProjector<
        T, P : PropertyType.State<T>,
        V : ViewModel, VS : ViewModel.Skeleton, VD,
        E : EditModel<T>, ES : EditModel.Skeleton, ED,
        >(
    scope: CoroutineScope,
    model: EditableModel<T, P, V, VS, VD, E, ES, ED>,
    dependencies: Dependencies,
) : ValueProjector {

    @Immutable
    @Pipe
    interface Dependencies {

        fun textView(): TextViewProjector.Dependencies

        fun textEdit(): TextEditProjector.Dependencies

    }

    sealed interface State {

        val mainCells: StateFlow<List<Cell>>

        data class View(
            val projector: ViewProjector,
            val edit: (() -> Unit)?,
        ) : State {

            override val mainCells: StateFlow<List<Cell>>
                get() = projector.mainCells
        }

        data class Edit(
            val projector: EditProjector,
            val save: StateFlow<StateFlow<(() -> Unit)?>?>,
            val cancel: () -> Unit,
        ) : State {

            override val mainCells: StateFlow<List<Cell>>
                get() = projector.mainCells
        }
    }

    private val state: StateFlow<State> = model
        .state
        .mapWithScope(scope) { stateScope, state ->
            when (state) {
                is EditableModel.State.View -> State.View(
                    edit = state.edit,
                    projector = when (val model = state.model) {
                        is TextViewModel -> TextViewProjector(
                            scope = stateScope,
                            dependencies = dependencies.textView(),
                            model = model
                        )
                    }
                )

                is EditableModel.State.Edit -> State.Edit(
                    save = state.save,
                    cancel = state.cancel,
                    projector = when (val model = state.model) {
                        is TextEditModel -> TextEditProjector(
                            scope = stateScope,
                            dependencies = dependencies.textEdit(),
                            model = model
                        )
                    }
                )
            }
        }

    override val topCells: StateFlow<List<Cell>> = state
        .scopedInState(scope)
        .flatMapState(scope) { (stateScope, state) ->
            when (state) {
                is State.Edit -> editTopCells(
                    scope = stateScope,
                    state = state,
                )

                is State.View -> viewTopCells(
                    scope = stateScope,
                    state = state,
                )
            }
        }

    private fun viewTopCells(
        scope: CoroutineScope,
        state: State.View,
    ): StateFlow<List<Cell>> = state
        .projector
        .topCells
        .mapState(scope) { topCells ->
            buildList {
                addAll(topCells)
                state.edit?.let { edit ->
                    add {
                        HnauButton(
                            shape = shape,
                            onClick = edit,
                        ) {
                            Icon(Icons.Filled.Edit)
                        }
                    }
                }
            }
        }

    private fun editTopCells(
        scope: CoroutineScope,
        state: State.Edit,
    ): StateFlow<List<Cell>> = state
        .projector
        .topCells
        .mapState(scope) { topCells ->
            buildList {
                addAll(topCells)
                add {
                    HnauButton(
                        onClick = state.cancel,
                        shape = shape,
                    ) {
                        Icon(Icons.Filled.Cancel)
                    }
                }
                add {
                    val saveOrNull by state.save.collectAsState()
                    val enabled = saveOrNull != null
                    val saveOrSaving = saveOrNull?.collectAsState()?.value
                    //TODO progress
                    HnauButton(
                        onClick = saveOrSaving,
                        shape = shape,
                    ) {
                        Icon(Icons.Filled.Done)
                    }
                }
            }
        }

    override val mainCells: StateFlow<List<Cell>> = state.flatMapState(scope) { state ->
        state.mainCells
    }
}