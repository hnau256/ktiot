package hnau.ktiot.client.projector.property.value

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import hnau.common.kotlin.coroutines.flatMapState
import hnau.common.kotlin.coroutines.mapState
import hnau.common.kotlin.coroutines.mapWithScope
import hnau.common.kotlin.foldNullable
import hnau.common.projector.uikit.HnauButton
import hnau.common.projector.uikit.table.Cell
import hnau.common.projector.utils.Icon
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

        val cells: StateFlow<List<Cell>>

        data class View(
            private val projector: ViewProjector,
            val edit: (() -> Unit)?,
        ) : State {

            override val cells: StateFlow<List<Cell>>
                get() = projector.cells
        }

        data class Edit(
            private val projector: EditProjector,
            val save: StateFlow<StateFlow<(() -> Unit)?>?>,
            val cancel: () -> Unit,
        ) : State {

            override val cells: StateFlow<List<Cell>>
                get() = projector.cells
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
        .mapState(scope) { state ->
            when (state) {
                is State.Edit -> editTopCells(state)
                is State.View -> viewTopCells(state)
            }
        }

    private fun viewTopCells(
        state: State.View,
    ): List<Cell> = state
        .edit
        .foldNullable(
            ifNull = {
                emptyList()
            },
            ifNotNull = { edit ->
                listOf {
                    HnauButton(
                        shape = shape,
                        onClick = edit,
                    ) {
                        Icon(Icons.Filled.Edit)
                    }
                }
            }
        )

    private fun editTopCells(
        state: State.Edit,
    ): List<Cell> = listOf(
        {
            HnauButton(
                onClick = state.cancel,
                shape = shape,
            ) {
                Icon(Icons.Filled.Cancel)
            }
        },
        {
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
    )

    override val mainCells: StateFlow<List<Cell>> = state.flatMapState(scope) {state ->
        state.cells
    }
}