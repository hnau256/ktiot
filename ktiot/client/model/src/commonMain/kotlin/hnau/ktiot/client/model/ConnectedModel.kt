@file:UseSerializers(
    MutableStateFlowSerializer::class,
)

package hnau.ktiot.client.model

import hnau.common.kotlin.getOrInit
import hnau.common.kotlin.serialization.MutableStateFlowSerializer
import hnau.common.kotlin.toAccessor
import hnau.common.app.model.goback.GoBackHandler
import hnau.ktiot.client.model.screen.ScreenModel
import hnau.ktiot.scheme.topic.MqttTopic
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

class ConnectedModel(
    scope: CoroutineScope,
    dependencies: Dependencies,
    skeleton: Skeleton,
) {

    @Pipe
    interface Dependencies {

        fun screen(): ScreenModel.Dependencies
    }

    @Serializable
    data class Skeleton(
        var rootScreen: ScreenModel.Skeleton? = null,
    )

    val rootScreen = ScreenModel(
        scope = scope,
        topic = MqttTopic.Absolute.root,
        dependencies = dependencies.screen(
        ),
        skeleton = skeleton::rootScreen
            .toAccessor()
            .getOrInit { ScreenModel.Skeleton() },
    )

    val goBackHandler: GoBackHandler
        get() = rootScreen.goBackHandler
}