package hnau.ktiot.client.projector

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import hnau.ktiot.client.model.ConnectedModel
import hnau.ktiot.client.model.utils.TemplateModel
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope

class ConnectedProjector(
    scope: CoroutineScope,
    model: ConnectedModel,
    dependencies: Dependencies,
) {

    @Pipe
    interface Dependencies {

    }

    @Composable
    fun Content() {
        Text("Connected")
    }
}