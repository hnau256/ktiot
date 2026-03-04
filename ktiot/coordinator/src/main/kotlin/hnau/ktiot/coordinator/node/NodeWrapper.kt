package hnau.ktiot.coordinator.node

import hnau.common.mqtt.types.topic.Topic
import hnau.ktiot.coordinator.utils.ElementWithChildren
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import org.hnau.commons.kotlin.Loadable
import org.hnau.commons.kotlin.Ready
import org.hnau.commons.kotlin.coroutines.flow.state.mapState
import org.hnau.commons.kotlin.coroutines.flow.state.mutable.toMutableStateFlowAsInitial

data class NodeWrapper<N>(
    val topic: Topic.Absolute,
    val node: N,
    val children: StateFlow<Loadable<List<ElementWithChildren<*>>>>,
) {

    data class Prototype<N>(
        val topic: Topic.Absolute,
        val node: N,
    ) {

        inline fun extractChildren(
            extract: (node: N) -> StateFlow<Loadable<List<ElementWithChildren<*>>>>,
        ): NodeWrapper<N> = NodeWrapper(
            topic = topic,
            node = node,
            children = extract(node),
        )

        inline fun extractChildren(
            scope: CoroutineScope,
            extract: (node: N) -> StateFlow<List<ElementWithChildren<*>>>,
        ): NodeWrapper<N> = extractChildren { node ->
            extract(node).mapState(scope, ::Ready)
        }

        inline fun extractChildrenList(
            scope: CoroutineScope,
            extract: (node: N) -> List<ElementWithChildren<*>>,
        ): NodeWrapper<N> = extractChildren(
            scope = scope,
        ) { node ->
            extract(node).toMutableStateFlowAsInitial()
        }
    }

    fun element(
        title: String = "",
        included: Boolean = false,
    ): ElementWithChildren<ElementWithChildren.Type.Child> = ElementWithChildren(
        topic = topic,
        type = ElementWithChildren.Type.Child(
            included = included,
            children = children,
        ),
        title = title,
    )

    fun includedElement(
        title: String = "",
    ): ElementWithChildren<ElementWithChildren.Type.Child> = element(
        included = true,
        title = title,
    )
}

inline fun <N> Topic.Absolute.node(
    createChild: (Topic.Absolute) -> N,
): NodeWrapper.Prototype<N> = NodeWrapper.Prototype(
    topic = this,
    node = createChild(this),
)