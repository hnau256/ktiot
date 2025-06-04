package hnau.ktiot.client.projector.property

import androidx.compose.runtime.Composable
import hnau.ktiot.client.model.property.PropertyModel
import hnau.ktiot.client.model.utils.TemplateModel
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope

class PropertyProjector(
    scope: CoroutineScope,
    model: PropertyModel,
    dependencies: Dependencies,
) {

    @Pipe
    interface Dependencies {

    }

    @Composable
    fun Content() {

    }
}