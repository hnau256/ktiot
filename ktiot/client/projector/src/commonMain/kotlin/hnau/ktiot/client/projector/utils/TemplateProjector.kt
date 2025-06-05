package hnau.ktiot.client.projector.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import hnau.ktiot.client.model.utils.TemplateModel
import hnau.pipe.annotations.Pipe
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope

private val logger = KotlinLogging.logger {  }

@Immutable
class TemplateProjector(
    scope: CoroutineScope,
    model: TemplateModel,
    dependencies: Dependencies,
) {

    @Immutable
    @Pipe
    interface Dependencies {

    }

    @Composable
    fun Content() {

    }
}