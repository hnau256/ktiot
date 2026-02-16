package hnau.ktiot.client.projector.property.value.editable

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import hnau.common.app.projector.uikit.TextInput
import hnau.common.app.projector.uikit.table.TableScope
import hnau.common.app.projector.uikit.utils.Dimens
import hnau.ktiot.client.model.property.value.editable.NumberEditModel
import hnau.ktiot.client.model.property.value.editable.NumberViewModel
import hnau.pipe.annotations.Pipe
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope

private val logger = KotlinLogging.logger { }

@Immutable
class NumberViewProjector(
    scope: CoroutineScope,
    private val model: NumberViewModel,
    dependencies: Dependencies,
) : ViewProjector {

    @Immutable
    @Pipe
    interface Dependencies

    @Composable
    override fun TableScope.Top() {
        Cell { modifier ->
            Text(
                modifier = modifier
                    .padding(Dimens.separation),
                text = model
                    .value
                    .collectAsState()
                    .value,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Immutable
class NumberEditProjector(
    scope: CoroutineScope,
    private val model: NumberEditModel,
    dependencies: Dependencies,
) : EditProjector {

    @Immutable
    @Pipe
    interface Dependencies

    @Composable
    override fun TableScope.Main() {
        Cell { modifier ->
            val focusRequester = remember { FocusRequester() }
            TextInput(
                modifier = modifier
                    .focusRequester(focusRequester),
                value = model.input,
                shape = shape,
            )
            LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
        }
    }
}