package hnau.ktiot.client.projector.property

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import hnau.ktiot.client.model.property.PropertyModel
import hnau.ktiot.client.model.utils.TemplateModel
import hnau.ktiot.scheme.topic.raw
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope

@Immutable
class PropertyProjector(
    scope: CoroutineScope,
    private val model: PropertyModel,
    dependencies: Dependencies,
) {

    @Immutable
    @Pipe
    interface Dependencies {

    }

    @Composable
    fun Content() {
        Text(model.topic.raw.topic)
    }
}