package hnau.ktiot.client.projector.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.configureSwingGlobalsForCompose
import hnau.common.kotlin.Loadable
import hnau.common.kotlin.coroutines.mapReusable
import hnau.common.kotlin.map
import hnau.common.model.goback.GlobalGoBackHandler
import hnau.common.model.goback.GoBackHandler
import hnau.common.projector.uikit.state.LoadableContent
import hnau.common.projector.uikit.state.TransitionSpec
import hnau.common.projector.utils.NavigationIcon
import hnau.ktiot.client.model.screen.ScreenItemModel
import hnau.ktiot.client.model.screen.ScreenModel
import hnau.ktiot.client.projector.property.PropertyProjector
import hnau.ktiot.scheme.topic.MqttTopic
import hnau.pipe.annotations.Pipe
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

@Immutable
class ScreenProjector(
    scope: CoroutineScope,
    model: ScreenModel,
    dependencies: Dependencies,
) {

    @Immutable
    @Pipe
    interface Dependencies {

        fun property(): PropertyProjector.Dependencies
    }

    @Immutable
    private data class Item(
        val topic: MqttTopic,
        val projector: ScreenItemProjector,
    )

    private val items: StateFlow<Loadable<ImmutableList<Item>>> = model
        .items
        .mapReusable(scope) { itemsOrLoading ->
            itemsOrLoading.map { items ->
                items
                    .map { item ->
                        getOrPutItem(item.topic) { itemScope ->
                            Item(
                                topic = item.topic,
                                projector = when (val itemModel = item.model) {
                                    is ScreenItemModel.Property -> ScreenItemProjector.Property(
                                        projector = PropertyProjector(
                                            scope = itemScope,
                                            dependencies = dependencies.property(),
                                            model = itemModel.model,
                                        )
                                    )
                                }
                            )
                        }
                    }
                    .toImmutableList()
            }
        }

    @Composable
    fun Content(
        contentPadding: PaddingValues,
    ) {

            items
                .collectAsState()
                .value
                .LoadableContent(
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = TransitionSpec.crossfade(),
                ) { items ->
                    Items(
                        items = items,
                        contentPadding = contentPadding,
                    )
                }
    }

    @Composable
    private fun Items(
        items: ImmutableList<Item>,
        contentPadding: PaddingValues,
    ) {
        LazyColumn(
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(
                items = items,
                key = { it.topic },
            ) {item ->
                item.projector.Content()
            }
        }
    }
}