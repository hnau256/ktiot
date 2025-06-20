package hnau.ktiot.client.projector.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import hnau.common.kotlin.Loadable
import hnau.common.kotlin.Loading
import hnau.common.kotlin.coroutines.createChild
import hnau.common.kotlin.coroutines.mapReusable
import hnau.common.kotlin.coroutines.runningFoldState
import hnau.common.kotlin.ifNull
import hnau.common.kotlin.map
import hnau.common.kotlin.valueOrElse
import hnau.common.projector.uikit.state.LoadableContent
import hnau.common.projector.uikit.state.TransitionSpec
import hnau.common.projector.uikit.utils.Dimens
import hnau.common.projector.utils.plus
import hnau.ktiot.client.model.screen.ScreenItemModel
import hnau.ktiot.client.model.screen.ScreenModel
import hnau.ktiot.client.projector.property.PropertyProjector
import hnau.ktiot.scheme.topic.ChildTopic
import hnau.ktiot.scheme.topic.raw
import hnau.pipe.annotations.Pipe
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
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
    private sealed interface State {

        @Immutable
        data class Child(
            val scope: CoroutineScope,
            val projector: ScreenProjector,
        ) : State

        @Immutable
        data class Items(
            val items: ImmutableList<Item>,
        ) : State {
            data class Item(
                val scope: CoroutineScope,
                val topic: ChildTopic,
                val projector: ScreenItemProjector,
            )
        }
    }

    private fun createState(
        scope: CoroutineScope,
        dependencies: Dependencies,
        cache: State?,
        itemsOrChildOrLoading: Loadable<Either<List<ScreenModel.Item>, ScreenModel>>,
    ): Loadable<State> = itemsOrChildOrLoading.map { itemsOrChild ->
        itemsOrChild.fold(
            ifRight = { childModel ->
                val (fromCache, itemsToCancel) = when (cache) {
                    is State.Child -> cache to null
                    is State.Items -> null to cache.items
                    null -> null to null
                }
                itemsToCancel?.forEach { it.scope.cancel() }
                fromCache.ifNull {
                    val childScope = scope.createChild()
                    State.Child(
                        scope = childScope,
                        projector = ScreenProjector(
                            scope = childScope,
                            dependencies = dependencies,
                            model = childModel,
                        )
                    )
                }
            },
            ifLeft = { itemsModels ->
                val (fromCache, childToCancel) = when (cache) {
                    is State.Child -> null to cache
                    is State.Items -> cache.items.associateBy { it.topic }.toMutableMap() to null
                    null -> null to null
                }
                childToCancel?.scope?.cancel()
                val result = State.Items(
                    items = itemsModels
                        .map { itemModel ->
                            fromCache?.remove(itemModel.topic).ifNull {
                                val itemScope = scope.createChild()
                                State.Items.Item(
                                    scope = itemScope,
                                    topic = itemModel.topic,
                                    projector = when (val itemModel = itemModel.model) {
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
                        .toImmutableList(),
                )
                fromCache?.forEach { (_, item) -> item.scope.cancel() }
                result
            },
        )
    }

    private val itemsOrChild: StateFlow<Loadable<State>> =
        model
            .itemsOrChild
            .runningFoldState(
                scope = scope,
                createInitial = { itemsOrChildOrLoading: Loadable<Either<List<ScreenModel.Item>, ScreenModel>> ->
                    createState(
                        scope = scope,
                        dependencies = dependencies,
                        cache = null,
                        itemsOrChildOrLoading = itemsOrChildOrLoading,
                    )
                },
                operation = { cache, itemsOrChildOrLoading ->
                    createState(
                        scope = scope,
                        dependencies = dependencies,
                        cache = cache.valueOrElse { null },
                        itemsOrChildOrLoading = itemsOrChildOrLoading,
                    )
                }
            )

    @Composable
    fun Content(
        contentPadding: PaddingValues,
    ) {
        itemsOrChild
            .collectAsState()
            .value
            .LoadableContent(
                modifier = Modifier.fillMaxSize(),
                transitionSpec = TransitionSpec.crossfade(),
            ) { state ->
                when (state) {
                    is State.Child -> state
                        .projector
                        .Content(contentPadding)
                    is State.Items -> Items(
                        items = state.items,
                        contentPadding = contentPadding,
                    )
                }
            }
    }

    @Composable
    private fun Items(
        items: ImmutableList<State.Items.Item>,
        contentPadding: PaddingValues,
    ) {
        LazyColumn(
            contentPadding = contentPadding + PaddingValues(Dimens.separation),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Dimens.separation),
        ) {
            items(
                items = items,
                key = { it.topic.topic.raw.topic },
            ) { item ->
                item.projector.Content(
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}