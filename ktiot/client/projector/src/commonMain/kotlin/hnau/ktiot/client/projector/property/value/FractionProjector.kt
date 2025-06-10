package hnau.ktiot.client.projector.property.value

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import arrow.core.nonEmptySetOf
import hnau.common.kotlin.foldBoolean
import hnau.ktiot.client.model.property.FractionModel
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope

@Immutable
class FractionProjector(
    scope: CoroutineScope,
    private val model: FractionModel,
    dependencies: Dependencies,
) : ValueProjector {

    @Immutable
    @Pipe
    interface Dependencies {

    }

    @Composable
    override fun Content() {
        val modifier = Modifier.fillMaxWidth()
        val range = model.type.range
        val value = model
            .value
            .collectAsState()
            .value
        model
            .mutable
            .foldBoolean(
                ifTrue = {
                    Slider(
                        modifier = modifier,
                        value = value,
                        valueRange = model.type.range,
                        onValueChange = model::update,
                        onValueChangeFinished = model::publish,
                        enabled = model
                            .isPublishing
                            .collectAsState()
                            .value
                            .not(),
                    )
                },
                ifFalse = {
                    val normalizedValue = remember(value, range) {
                        range
                            .takeIf { !it.isEmpty() }
                            ?.let { nonEmptyRange ->
                                (value - nonEmptyRange.start) /
                                        (nonEmptyRange.endInclusive - nonEmptyRange.start)
                            }
                            ?.fastCoerceIn(0f, 1f)
                            ?: 0f
                    }
                    LinearProgressIndicator(
                        modifier = modifier,
                        progress = { normalizedValue },
                    )
                }
            )

    }
}