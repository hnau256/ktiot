package hnau.ktiot.client.projector.property.value

import androidx.compose.runtime.Composable
import hnau.common.kotlin.coroutines.toMutableStateFlowAsInitial
import hnau.common.projector.uikit.table.Cell
import hnau.common.projector.uikit.table.CellScope
import kotlinx.coroutines.flow.StateFlow

sealed interface ValueProjector {

    val topCells: StateFlow<List<Cell>>
        get() = emptyList<Cell>().toMutableStateFlowAsInitial()

    @Composable
    fun MainContent()
}