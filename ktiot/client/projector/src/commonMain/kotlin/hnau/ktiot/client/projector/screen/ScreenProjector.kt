package hnau.ktiot.client.projector.screen

import androidx.compose.runtime.Composable
import hnau.common.kotlin.coroutines.flatMapState
import hnau.common.kotlin.coroutines.scopedInState
import hnau.ktiot.client.model.screen.ScreenModel
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope

class ScreenProjector(
    scope: CoroutineScope,
    model: ScreenModel,
    dependencies: Dependencies,
) {

    @Pipe
    interface Dependencies {

    }

    /*private val items = model
        .items
        .flatMapState(scope) {itemsScope, itemsOrLoading ->

        }*/

    @Composable
    fun Content() {

    }
}