package hnau.ktiot.client.projector.property.value.editable

import hnau.common.app.projector.uikit.table.Cell
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface ViewProjector {

    val topCells: StateFlow<List<Cell>>
        get() = MutableStateFlow(emptyList())

    val mainCells: StateFlow<List<Cell>>
        get() = MutableStateFlow(emptyList())
}

sealed interface EditProjector {

    val topCells: StateFlow<List<Cell>>
        get() = MutableStateFlow(emptyList())

    val mainCells: StateFlow<List<Cell>>
        get() = MutableStateFlow(emptyList())
}