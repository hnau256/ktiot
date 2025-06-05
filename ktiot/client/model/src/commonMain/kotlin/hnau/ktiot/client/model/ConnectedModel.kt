@file:UseSerializers(
    MutableStateFlowSerializer::class,
)

package hnau.ktiot.client.model

import hnau.common.kotlin.getOrInit
import hnau.common.model.goback.GoBackHandler
import hnau.common.model.goback.GoBackHandlerProvider
import hnau.common.kotlin.serialization.MutableStateFlowSerializer
import hnau.common.kotlin.toAccessor
import hnau.common.model.goback.NeverGoBackHandler
import hnau.common.mqtt.utils.MqttClient
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

        fun screen(
            topic: MqttTopic.Absolute,
        ): ScreenModel.Dependencies
    }

    @Serializable
    data class Skeleton(
        var rootScreen: ScreenModel.Skeleton? = null,
    )

    val rootScreen = ScreenModel(
        scope = scope,
        dependencies = dependencies.screen(
            topic = MqttTopic.Absolute.root,
        ),
        skeleton = skeleton::rootScreen
            .toAccessor()
            .getOrInit { ScreenModel.Skeleton() },
    )

    val goBackHandler: GoBackHandler
        get() = rootScreen.goBackHandler
}