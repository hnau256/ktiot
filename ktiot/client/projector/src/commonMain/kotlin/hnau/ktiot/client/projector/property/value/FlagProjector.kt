package hnau.ktiot.client.projector.property.value

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import hnau.common.app.projector.uikit.utils.Dimens
import hnau.common.kotlin.foldBoolean
import hnau.ktiot.client.model.property.value.FlagModel
import hnau.ktiot.client.projector.utils.Localization
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope

@Immutable
class FlagProjector(
    scope: CoroutineScope,
    private val model: FlagModel,
    private val dependencies: Dependencies,
) : ValueProjector {

    @Immutable
    @Pipe
    interface Dependencies {

        val localization: Localization
    }

    @Composable
    override fun Main() {
    }

    @Composable
    override fun Top() {

        val value = model
            .value
            .collectAsState()
            .value

        model
            .mutable
            .foldBoolean(
                ifFalse = {
                    Text(
                        modifier = Modifier.padding(
                            horizontal = Dimens.separation,
                            vertical = Dimens.smallSeparation,
                        ),
                        text = value.foldBoolean(
                            ifTrue = { dependencies.localization.yes },
                            ifFalse = { dependencies.localization.no }
                        ),
                        color = value.foldBoolean(
                            ifTrue = { MaterialTheme.colorScheme.primary },
                            ifFalse = { MaterialTheme.colorScheme.error }
                        ),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                ifTrue = {
                    Switch(
                        modifier = Modifier.padding(
                            horizontal = Dimens.separation,
                            vertical = Dimens.smallSeparation,
                        ),
                        checked = value,
                        onCheckedChange = { model.publish.value?.invoke(it) },
                        enabled = model.publish.collectAsState().value != null,
                    )
                }
            )
    }
}