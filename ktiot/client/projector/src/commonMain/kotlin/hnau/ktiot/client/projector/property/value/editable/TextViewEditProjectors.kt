package hnau.ktiot.client.projector.property.value.editable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import hnau.common.projector.uikit.TextInput
import hnau.ktiot.client.model.property.value.editable.TextEditModel
import hnau.ktiot.client.model.property.value.editable.TextViewModel
import hnau.pipe.annotations.Pipe
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope

private val logger = KotlinLogging.logger {  }

@Immutable
class TextViewProjector(
    scope: CoroutineScope,
    private val model: TextViewModel,
    dependencies: Dependencies,
): ViewProjector {

    @Immutable
    @Pipe
    interface Dependencies

    @Composable
    override fun Content() {
        Text(
            modifier = Modifier
                .fillMaxWidth(),
            text = model
                .value
                .collectAsState()
                .value,
        )
    }
}

@Immutable
class TextEditProjector(
    scope: CoroutineScope,
    private val model: TextEditModel,
    dependencies: Dependencies,
): EditProjector {

    @Immutable
    @Pipe
    interface Dependencies

    @Composable
    override fun Content() {
        val focusRequester = remember { FocusRequester() }
        TextInput(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            value = model.input,
        )
        LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
    }
}