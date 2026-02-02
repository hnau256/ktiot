package hnau.ktiot.client.projector.property.value

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import hnau.common.app.projector.uikit.table.TableScope
import hnau.common.app.projector.utils.Icon
import hnau.common.kotlin.coroutines.flow.state.mapWithScope
import hnau.ktiot.client.model.property.value.EditableModel
import hnau.ktiot.client.model.property.value.editable.EditModel
import hnau.ktiot.client.model.property.value.editable.TextEditModel
import hnau.ktiot.client.model.property.value.editable.TextViewModel
import hnau.ktiot.client.model.property.value.editable.ViewModel
import hnau.ktiot.client.projector.property.value.editable.EditProjector
import hnau.ktiot.client.projector.property.value.editable.TextEditProjector
import hnau.ktiot.client.projector.property.value.editable.TextViewProjector
import hnau.ktiot.client.projector.property.value.editable.ViewProjector
import hnau.ktiot.client.projector.property.value.utils.TopMainProjector
import hnau.ktiot.client.projector.utils.Button
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

        val projector: TopMainProjector

        data class View(
            override val projector: ViewProjector,
            val edit: (() -> Unit)?,
        ) : State

        data class Edit(
            override val projector: EditProjector,
            val save: StateFlow<StateFlow<(() -> Unit)?>?>,
            val cancel: () -> Unit,
        ) : State
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

    @Composable
    override fun TableScope.Top() {
        val state by state.collectAsState()
        when (val state = state) {
            is State.Edit -> EditTopCells(
                state = state,
            )

            is State.View -> ViewTopCells(
                state = state,
            )
        }
    }

    @Composable
    override fun TableScope.Main() {
        val state by state.collectAsState()
        with(state.projector) { Main() }
    }

    @Composable
    private fun TableScope.ViewTopCells(
        state: State.View,
    ) {
        val editOrNull = state.edit
        with(state.projector) {
            Top()
        }
        editOrNull?.let { edit ->
            Cell { modifier ->
                Button(
                    shape = shape,
                    modifier = modifier,
                    onClick = edit,
                ) {
                    Icon(Icons.Filled.Edit)
                }
            }
        }
    }

    @Composable
    private fun TableScope.EditTopCells(
        state: State.Edit,
    ) {
        with(state.projector) {
            Top()
        }
        Cell { modifier ->
            Button(
                shape = shape,
                modifier = modifier,
                onClick = state.cancel,
            ) {
                Icon(Icons.Filled.Cancel)
            }
        }

        Cell { modifier ->
            val saveOrCancel by state.save.collectAsState()
            saveOrCancel.Button(
                shape = shape,
                modifier = modifier,
                content = { Icon(Icons.Filled.Done) },
            )
        }
    }
}