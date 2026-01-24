package hnau.ktiot.coordinator.utils

import hnau.common.kotlin.Loadable
import hnau.common.kotlin.coroutines.flow.state.scopedInState
import hnau.common.kotlin.fold
import hnau.common.kotlin.map
import hnau.common.kotlin.mapSecond
import hnau.common.mqtt.utils.MqttClient
import hnau.ktiot.scheme.Element
import hnau.ktiot.scheme.SchemeConstants
import hnau.ktiot.scheme.topic.MqttTopic
import hnau.ktiot.scheme.topic.ktiotElements
import hnau.ktiot.scheme.topic.raw
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal fun MqttClient.publishScheme(
    scope: CoroutineScope,
    rootElements: StateFlow<Loadable<List<ElementWithChildren<*>>>>,
) {
    publishElements(
        scope = scope,
        elements = rootElements,
        topic = MqttTopic.Absolute.root,
    )
}

private fun MqttClient.publishElements(
    scope: CoroutineScope,
    topic: MqttTopic.Absolute,
    elements: StateFlow<Loadable<List<ElementWithChildren<*>>>>,
) {
    scope.launch {
        elements
            .scopedInState(scope)
            .collectLatest { (scope, elementsOrLoading) ->

                val elementsWithChildrenOrLoading = elementsOrLoading.map { elements ->
                    elements.map { elementWithChildren ->
                        elementWithChildren
                            .toElement(
                                parent = topic,
                            )
                            .mapSecond { childrenOrNull ->
                                childrenOrNull?.let { children ->
                                    val topic = elementWithChildren.topic
                                    topic to children
                                }
                            }
                    }
                }

                elementsWithChildrenOrLoading.fold(
                    ifLoading = {},
                    ifReady = { elementsWithChildren ->
                        elementsWithChildren
                            .mapNotNull { (_, topicWithChildren) -> topicWithChildren }
                            .forEach { (topic, children) ->
                                publishElements(
                                    scope = scope,
                                    topic = topic,
                                    elements = children,
                                )
                            }
                    }
                )

                publish(
                    topic = topic.ktiotElements.raw,
                    retained = true,
                    payload = SchemeConstants.mapper.reverse(
                        elementsWithChildrenOrLoading.map { elementsWithChildren ->
                            elementsWithChildren.map(Pair<Element, *>::first)
                        }
                    ),
                )
            }
    }
}
