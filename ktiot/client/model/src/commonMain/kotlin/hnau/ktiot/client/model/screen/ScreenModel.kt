@file:UseSerializers(
    MutableStateFlowSerializer::class,
)

package hnau.ktiot.client.model.screen

import arrow.core.Either
import arrow.core.identity
import arrow.core.toNonEmptyListOrNull
import org.hnau.commons.app.model.goback.GoBackHandler
import org.hnau.commons.app.model.goback.NeverGoBackHandler
import org.hnau.commons.kotlin.*
import org.hnau.commons.kotlin.coroutines.flow.state.*
import org.hnau.commons.kotlin.coroutines.flow.state.mutable.toMutableStateFlowAsInitial
import org.hnau.commons.kotlin.serialization.MutableStateFlowSerializer
import org.hnau.commons.logging.tryOrLog
import org.hnau.commons.mqtt.utils.MqttClient
import hnau.ktiot.client.model.property.PropertyModel
import hnau.ktiot.client.model.property.toTitle
import hnau.ktiot.client.model.utils.ChildTopic
import hnau.ktiot.client.model.utils.MutableMapSerializer
import hnau.ktiot.client.model.utils.asChild
import hnau.ktiot.scheme.Element
import hnau.ktiot.scheme.SchemeConstants
import hnau.ktiot.scheme.topic.MqttTopic
import hnau.ktiot.scheme.topic.ktiotElements
import hnau.ktiot.scheme.topic.raw
import org.hnau.commons.gen.pipe.annotations.Pipe
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

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

    fun openChild(
        topic: ChildTopic,
    ) {
        skeleton
            .selectedChild
            .value = topic.topic to Skeleton()
    }

    data class Item(
        val model: ScreenItemModel,
        val topic: ChildTopic,
    )

    val itemsOrChild: StateFlow<Loadable<Either<List<Item>, ScreenModel>>> = combineState(
        scope = scope,
        first = createItemsForTopic(
            topic = topic,
            scope = scope,
        ),
        second = skeleton.selectedChild,
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
                            SchemeConstants.mapper.direct(message.payload)
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
                    elementsOrLoading.map { elementsOrLoading ->
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
                .mapState(scope) { elementsOrLoading2 ->
                    elementsOrLoading2.flatMap(::it)
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
                                    first = acc,
                                    second = nextModels,
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
    ): StateFlow<List<Item>> {
        val topic = element.topic.asChild(parentTopic)

        val title = element
            .title
            .takeIf(String::isNotBlank)
            .ifNull(topic::toTitle)

        return when (val type = element.type) {
            is Element.Type.Property<*> -> createPropertyItem(
                scope = scope,
                topic = topic,
                title = title,
                element = type,
            )
                .let(::listOf)
                .toMutableStateFlowAsInitial()

            is Element.Type.Child -> when (type.included) {
                true -> createItemsForTopic(
                    topic = topic.topic,
                    scope = scope,
                )
                    .mapState(scope) { itemsOrLoading ->
                        itemsOrLoading.fold(
                            ifLoading = { emptyList() },
                            ifReady = ::identity,
                        )
                    }

                false -> Item(
                    topic = element.topic.asChild(parentTopic),
                    model = ScreenItemModel.ChildButton(
                        title = title,
                    ),
                )
                    .let(::listOf)
                    .toMutableStateFlowAsInitial()
            }
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
        scope: CoroutineScope,
        topic: ChildTopic,
        title: String,
        element: Element.Type.Property<T>,
    ): Item {
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
                title = title,
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