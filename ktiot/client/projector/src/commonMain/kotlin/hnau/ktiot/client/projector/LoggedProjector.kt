package hnau.ktiot.client.projector

import androidx.compose.runtime.Composable
import hnau.ktiot.client.model.LoggedModel
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope

class LoggedProjector(
    scope: CoroutineScope,
    model: LoggedModel,
    dependencies: Dependencies,
) {

    @Pipe
    interface Dependencies {

    }

    @Composable
    fun Content() {

    }
}