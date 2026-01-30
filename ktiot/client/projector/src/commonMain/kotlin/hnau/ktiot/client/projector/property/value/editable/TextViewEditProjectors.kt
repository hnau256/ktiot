package hnau.ktiot.client.projector.property.value.editable

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import hnau.common.app.projector.uikit.TextInput
import hnau.common.app.projector.uikit.table.Cell
import hnau.common.app.projector.uikit.table.CellBox
import hnau.common.app.projector.uikit.utils.Dimens
import hnau.ktiot.client.model.property.value.editable.TextEditModel
import hnau.ktiot.client.model.property.value.editable.TextViewModel
import hnau.pipe.annotations.Pipe
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private val logger = KotlinLogging.logger { }

@Immutable
class TextViewProjector(
    scope: CoroutineScope,
    private val model: TextViewModel,
    dependencies: Dependencies,
) : ViewProjector {

    @Immutable
    @Pipe
    interface Dependencies

    override val topCells: StateFlow<List<Cell>> = MutableStateFlow(
        listOf {
            CellBox {
                Text(
                    modifier = Modifier
                        .padding(Dimens.separation),
                    text = model
                        .value
                        .collectAsState()
                        .value,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    )
}

@Immutable
class TextEditProjector(
    scope: CoroutineScope,
    private val model: TextEditModel,
    dependencies: Dependencies,
) : EditProjector {

    @Immutable
    @Pipe
    interface Dependencies

    override val mainCells: StateFlow<List<Cell>> = MutableStateFlow(
        listOf {
            val focusRequester = remember { FocusRequester() }
            TextInput(
                shape = shape,
                modifier = Modifier
                    .focusRequester(focusRequester),
                value = model.input,
            )
            LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
        }
    )
}