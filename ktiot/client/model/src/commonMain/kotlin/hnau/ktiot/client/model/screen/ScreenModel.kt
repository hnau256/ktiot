package hnau.ktiot.client.model.screen

import arrow.core.toNonEmptyListOrNull
import hnau.common.kotlin.Loadable
import hnau.common.kotlin.Loading
import hnau.common.kotlin.Ready
import hnau.common.kotlin.coroutines.flatMapState
import hnau.common.kotlin.coroutines.mapReusable
import hnau.common.kotlin.coroutines.scopedInState
import hnau.common.kotlin.coroutines.toMutableStateFlowAsInitial
import hnau.common.kotlin.fold
import hnau.common.kotlin.foldNullable
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
    private val topic: MqttTopic.Absolute
) {

    @Pipe
    interface Dependencies {

        val mqttClient: MqttClient

        fun property(): PropertyModel.Dependencies
    }

    @Serializable
    data class Skeleton(
        @Serializable(MutableMapSerializer::class)
        val items: MutableMap<MqttTopic, ScreenItemModel.Skeleton> = HashMap(),
    )

    data class Item(
        val model: ScreenItemModel,
        val topic: ChildTopic,
    )

    val items: StateFlow<Loadable<List<Item>>> = topic
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
                                val itemModel = createItem(
                                    scope = elementScope,
                                    element = element,
                                )
                                Item(
                                    model = itemModel,
                                    topic = itemTopic,
                                )
                            }
                        }
                    }
                }
        }

    private fun createItem(
        scope: CoroutineScope,
        element: Element,
    ): ScreenItemModel = when (element) {
        is Element.Property<*> -> createPropertyItem(
            scope = scope,
            element = element,
        )
    }

    private inline fun <reified T : ScreenItemModel.Skeleton> getOrCreateItemSkeleton(
        topic: MqttTopic,
        create: () -> T,
    ): T = skeleton.items.let { items ->
        var result = items[topic] as? T
        if (result == null) {
            result = create()
            items[topic] = result
        }
        result
    }

    private fun <T> createPropertyItem(
        scope: CoroutineScope,
        element: Element.Property<T>,
    ): ScreenItemModel = ScreenItemModel.Property(
        PropertyModel(
            scope = scope,
            skeleton = getOrCreateItemSkeleton(
                topic = element.topic,
                create = {
                    ScreenItemModel.Skeleton.Property(
                        PropertyModel.Skeleton()
                    )
                }
            ).skeleton,
            dependencies = dependencies.property(),
            topic = element.topic.asChild(topic),
            property = element,
        )
    )

    val goBackHandler: GoBackHandler = items
        .scopedInState(scope)
        .flatMapState(scope) { (itemsScope, itemsOrLoading) ->
            itemsOrLoading.fold(
                ifLoading = { NeverGoBackHandler },
                ifReady = { items ->
                    items
                        .asReversed()
                        .createGoBackHandler(itemsScope)
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