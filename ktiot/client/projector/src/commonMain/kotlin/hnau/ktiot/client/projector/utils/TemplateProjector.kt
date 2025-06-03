package hnau.ktiot.client.projector.utils

import androidx.compose.runtime.Composable
import hnau.ktiot.client.model.utils.TemplateModel
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope

class TemplateProjector(
    scope: CoroutineScope,
    model: TemplateModel,
    dependencies: Dependencies,
) {

    @Pipe
    interface Dependencies {

    }

    @Composable
    fun Content() {

    }
}