package hnau.ktiot.client.projector.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import hnau.common.kotlin.Loadable
import hnau.common.kotlin.coroutines.mapWithScope
import hnau.common.kotlin.fold
import hnau.common.kotlin.map
import hnau.common.projector.uikit.ErrorPanel
import hnau.common.projector.uikit.progressindicator.ProgressIndicatorInBox
import hnau.common.projector.uikit.state.LoadableContent
import hnau.common.projector.uikit.state.StateContent
import hnau.common.projector.uikit.state.TransitionSpec
import hnau.common.projector.uikit.utils.Dimens
import hnau.common.projector.utils.Icon
import hnau.ktiot.client.model.property.FractionModel
import hnau.ktiot.client.model.property.PropertyModel
import hnau.ktiot.client.projector.property.value.FractionProjector
import hnau.ktiot.client.projector.property.value.ValueProjector
import hnau.ktiot.client.projector.utils.icon
import hnau.ktiot.client.projector.utils.toTitle
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

@Immutable
class PropertyProjector(
    scope: CoroutineScope,
    private val model: PropertyModel,
    dependencies: Dependencies,
) {

    @Immutable
    @Pipe
    interface Dependencies {

        fun fraction(): FractionProjector.Dependencies
    }

    private val value: StateFlow<Loadable<Result<ValueProjector>>> = model.value.mapWithScope(
        scope = scope,
    ) { valueScope, valueOrErrorOrLoading ->
        valueOrErrorOrLoading.map { valueOrError ->
            valueOrError.map { value ->
                when (value) {
                    is FractionModel -> FractionProjector(
                        scope = valueScope,
                        model = value,
                        dependencies = dependencies.fraction(),
                    )
                }
            }
        }
    }

    @Composable
    fun Content(
        modifier: Modifier,
    ) {
        Card(
            modifier = modifier,
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = Dimens.separation,
                    vertical = Dimens.separation,
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.separation)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.smallSeparation),
                ) {
                    Icon(
                        icon = model.mode.icon,
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = model.topic.toTitle(),
                        style = MaterialTheme.typography.titleMedium,
                    )

                }
                Value()
            }
        }
    }

    @Composable
    private fun Value() {
        value
            .collectAsState()
            .value
            .StateContent(
                label = "propertyValueOrErrorOrLoading",
                transitionSpec = TransitionSpec.crossfade(),
                contentKey = { valueOrErrorOrLoading ->
                    valueOrErrorOrLoading.fold(
                        ifLoading = { 0 },
                        ifReady = { valueOrError ->
                            valueOrError.fold(
                                onFailure = { 1 },
                                onSuccess = { 2 },
                            )
                        }
                    )
                },
            ) { valueOrErrorOrLoading ->
                valueOrErrorOrLoading.fold(
                    ifLoading = { ProgressIndicatorInBox() },
                    ifReady = { valueOrError ->
                        valueOrError.fold(
                            onFailure = { error ->
                                ErrorPanel(
                                    title = {
                                        Text(
                                            text = error.message.toString(),
                                        )
                                    }
                                )
                            },
                            onSuccess = { valueProjector ->
                                valueProjector.Content()
                            },
                        )
                    }
                )
            }
    }
}