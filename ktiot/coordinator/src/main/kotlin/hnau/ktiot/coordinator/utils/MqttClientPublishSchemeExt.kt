package hnau.ktiot.coordinator.utils

import hnau.common.mqtt.types.MqttSession
import hnau.common.mqtt.types.topic.Topic
import hnau.common.mqtt.types.topic.ktiotElements
import hnau.ktiot.scheme.Element
import hnau.ktiot.scheme.SchemeConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.hnau.commons.kotlin.Loadable
import org.hnau.commons.kotlin.coroutines.flow.state.scopedInState
import org.hnau.commons.kotlin.fold
import org.hnau.commons.kotlin.map
import org.hnau.commons.kotlin.mapSecond

internal fun MqttSession.publishScheme(
    scope: CoroutineScope,
    rootElements: StateFlow<Loadable<List<ElementWithChildren<*>>>>,
) {
    publishElements(
        scope = scope,
        elements = rootElements,
        topic = Topic.Absolute.root,
    )
}

private fun MqttSession.publishElements(
    scope: CoroutineScope,
    topic: Topic.Absolute,
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
                    topic = topic.ktiotElements,
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
