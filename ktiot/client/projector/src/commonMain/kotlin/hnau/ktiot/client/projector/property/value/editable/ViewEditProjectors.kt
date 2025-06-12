package hnau.ktiot.client.projector.property.value.editable

import hnau.common.projector.uikit.table.Cell
import kotlinx.coroutines.flow.StateFlow

sealed interface ViewProjector {

    val cells: StateFlow<List<Cell>>
}

sealed interface EditProjector {

    val cells: StateFlow<List<Cell>>
}