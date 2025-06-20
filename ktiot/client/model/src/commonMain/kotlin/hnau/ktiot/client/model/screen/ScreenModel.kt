package hnau.ktiot.client.model.screen

import arrow.core.Either
import arrow.core.identity
import arrow.core.toNonEmptyListOrNull
import hnau.common.kotlin.Loadable
import hnau.common.kotlin.Loading
import hnau.common.kotlin.Ready
import hnau.common.kotlin.coroutines.combineState
import hnau.common.kotlin.coroutines.flatMapState
import hnau.common.kotlin.coroutines.mapReusable
import hnau.common.kotlin.coroutines.mapState
import hnau.common.kotlin.coroutines.mapWithScope
import hnau.common.kotlin.coroutines.scopedInState
import hnau.common.kotlin.coroutines.toMutableStateFlowAsInitial
import hnau.common.kotlin.fold
import hnau.common.kotlin.foldNullable
import hnau.common.kotlin.ifNull
import hnau.common.kotlin.map
import hnau.common.logging.tryOrLog
import hnau.common.model.goback.GoBackHandler
import hnau.common.model.goback.NeverGoBackHandler
import hnau.common.mqtt.utils.MqttClient
import hnau.ktiot.client.model.property.PropertyModel
import hnau.ktiot.client.model.utils.MutableMapSerializer
import hnau.ktiot.scheme.Element
import hnau.ktiot.scheme.topic.ChildTopic
import hnau.ktiot.scheme.topic.MqttTopic
import hnau.ktiot.scheme.topic.asChild
import hnau.ktiot.scheme.topic.ktiotElements
import hnau.ktiot.scheme.topic.raw
import hnau.pipe.annotations.Pipe
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable

private val logger = KotlinLogging.logger { }

class ScreenModel(
    scope: CoroutineScope,
    private val dependencies: Dependencies,
    private val skeleton: Skeleton,
    topic: MqttTopic.Absolute,
) {

    @Pipe
    interface Dependencies {

        val mqttClient: MqttClient

        fun property(): PropertyModel.Dependencies
    }

    @Serializable
    data class Skeleton(
        val selectedChild: MutableStateFlow<Pair<MqttTopic.Absolute, Skeleton>?> =
            null.toMutableStateFlowAsInitial(),

        @Serializable(MutableMapSerializer::class)
        val items: MutableMap<MqttTopic, ScreenItemModel.Skeleton> = HashMap(),
    )

    data class Item(
        val model: ScreenItemModel,
        val topic: ChildTopic,
    )

    val itemsOrChild: StateFlow<Loadable<Either<List<Item>, ScreenModel>>> = combineState(
        scope = scope,
        a = createItemsForTopic(
            topic = topic,
            scope = scope,
        ),
        b = skeleton.selectedChild,
    ) { itemsOrLoading, selectedChildOrNull ->
        itemsOrLoading.map { items ->
            selectedChildOrNull
                ?.takeIf { selectedChild ->
                    val selectedTopic = selectedChild.first
                    items.any { it.topic.topic == selectedTopic }
                }
                .foldNullable(
                    ifNull = { Either.Left(items) },
                    ifNotNull = { selectedChild ->
                        Either.Right(selectedChild)
                    }
                )
        }
    }
        .mapWithScope(scope) { stateScope, itemsOrSelectedChildOrLoading ->
            itemsOrSelectedChildOrLoading.map { itemsOrChildSkeleton ->
                itemsOrChildSkeleton.map { (childTopic, childSkeleton) ->
                    ScreenModel(
                        scope = stateScope,
                        skeleton = childSkeleton,
                        dependencies = dependencies,
                        topic = childTopic,
                    )
                }
            }
        }

    private fun createItemsForTopic(
        scope: CoroutineScope,
        topic: MqttTopic.Absolute,
    ): StateFlow<Loadable<List<Item>>> = topic
        .ktiotElements
        .raw
        .let { ktIoTTopic ->
            dependencies
                .mqttClient
                .subscribe(
                    topic = ktIoTTopic,
                )
                .mapNotNull { message ->
                    logger
                        .tryOrLog(
                            log = "parsing ktiot elements from '$ktIoTTopic' from $message"
                        ) {
                            Element.Companion.listMqttPayloadMapper.direct(message.payload)
                        }
                        .getOrNull()
                }
                .map(::Ready)
                .stateIn(
                    scope = scope,
                    started = SharingStarted.Companion.Eagerly,
                    initialValue = Loading,
                )
                .mapReusable(scope) { elementsOrLoading ->
                    elementsOrLoading.map { elements ->
                        elements.map { element ->
                            val itemTopic = element.topic.asChild(topic)
                            getOrPutItem(
                                key = itemTopic,
                            ) { elementScope ->
                                createItems(
                                    parentTopic = topic,
                                    scope = elementScope,
                                    element = element,
                                )
                            }
                        }
                    }
                }
        }
        .scopedInState(scope)
        .flatMapState(scope) { (stateScope, stateOrLoading) ->
            stateOrLoading.fold(
                ifLoading = { Loading.toMutableStateFlowAsInitial() },
                ifReady = { items ->
                    items
                        .toNonEmptyListOrNull()
                        ?.let { nonEmptyItems ->
                            nonEmptyItems.tail.fold(
                                initial = nonEmptyItems.head,
                            ) { acc, nextModels ->
                                combineState(
                                    scope = stateScope,
                                    a = acc,
                                    b = nextModels,
                                ) { currentAcc, currentNextModels ->
                                    currentAcc + currentNextModels
                                }
                            }
                        }
                        .ifNull { emptyList<Item>().toMutableStateFlowAsInitial() }
                        .mapState(stateScope) { Ready(it) }
                }
            )
        }

    private fun createItems(
        parentTopic: MqttTopic.Absolute,
        scope: CoroutineScope,
        element: Element,
    ): StateFlow<List<Item>> = when (element) {
        is Element.Property<*> -> createPropertyItem(
            parentTopic = parentTopic,
            scope = scope,
            element = element,
        )
            .let(::listOf)
            .toMutableStateFlowAsInitial()

        is Element.Include -> createItemsForTopic(
            topic = element.topic.asChild(parentTopic).topic,
            scope = scope,
        ).mapState(scope) { itemsOrLoading ->
            itemsOrLoading.fold(
                ifLoading = { emptyList() },
                ifReady = ::identity,
            )
        }
    }

    private inline fun <reified T : ScreenItemModel.Skeleton> getOrCreateItemSkeleton(
        topic: ChildTopic,
        create: () -> T,
    ): T = skeleton.items.let { items ->
        var result = items[topic.topic] as? T
        if (result == null) {
            result = create()
            items[topic.topic] = result
        }
        result
    }

    private fun <T> createPropertyItem(
        parentTopic: MqttTopic.Absolute,
        scope: CoroutineScope,
        element: Element.Property<T>,
    ): Item {
        val topic = element.topic.asChild(parentTopic)
        val model = ScreenItemModel.Property(
            PropertyModel(
                scope = scope,
                skeleton = getOrCreateItemSkeleton(
                    topic = topic,
                    create = {
                        ScreenItemModel.Skeleton.Property(
                            PropertyModel.Skeleton()
                        )
                    }
                ).skeleton,
                dependencies = dependencies.property(),
                topic = topic,
                property = element,
            )
        )
        return Item(
            topic = topic,
            model = model,
        )
    }

    val goBackHandler: GoBackHandler = itemsOrChild
        .scopedInState(scope)
        .flatMapState(scope) { (stateScope, stateOrLoading) ->
            stateOrLoading.fold(
                ifLoading = { NeverGoBackHandler },
                ifReady = { itemsOrChild ->
                    itemsOrChild.fold(
                        ifLeft = { items ->
                            items
                                .asReversed()
                                .createGoBackHandler(stateScope)
                        },
                        ifRight = { selectedChild ->
                            selectedChild
                                .goBackHandler
                                .mapState(stateScope) { childGoBack ->
                                    childGoBack.foldNullable(
                                        ifNull = { { skeleton.selectedChild.value = null } },
                                        ifNotNull = ::identity,
                                    )
                                }
                        }
                    )
                }
            )
        }

    private fun List<Item>.createGoBackHandler(
        scope: CoroutineScope,
    ): GoBackHandler = toNonEmptyListOrNull().foldNullable(
        ifNull = { NeverGoBackHandler },
        ifNotNull = { nonEmptyItems ->
            nonEmptyItems
                .head
                .model
                .goBackHandler
                .scopedInState(scope)
                .flatMapState(scope) { (headScope, headGoBackHandler) ->
                    headGoBackHandler.foldNullable(
                        ifNotNull = { it.toMutableStateFlowAsInitial() },
                        ifNull = {
                            nonEmptyItems.tail.createGoBackHandler(
                                scope = headScope,
                            )
                        }
                    )
                }
        }
    )
}