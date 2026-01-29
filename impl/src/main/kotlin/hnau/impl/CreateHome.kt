package hnau.impl

import hnau.ktiot.coordinator.asReadyStateFlow
import hnau.ktiot.coordinator.utils.ElementWithChildren
import hnau.ktiot.scheme.topic.MqttTopic
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope

class Home(
    scope: CoroutineScope,
    topic: MqttTopic.Absolute,
    dependencies: Dependencies,
) {


    @Pipe
    interface Dependencies {

        fun insectsBlocker(): InsectsBlocker.Dependencies

        companion object
    }

    val children: List<ElementWithChildren<ElementWithChildren.Type.Child>> = (0..1).map { index ->

        val childTopic = topic + "room_${index + 1}"

        val blocker = InsectsBlocker(
            scope = scope,
            topic = childTopic,
            dependencies = dependencies.insectsBlocker(),
        )

        ElementWithChildren(
            topic = childTopic,
            type = ElementWithChildren.Type.Child(
                included = false,
                children = blocker
                    .children
                    .asReadyStateFlow(),
            )
        )
    }
}