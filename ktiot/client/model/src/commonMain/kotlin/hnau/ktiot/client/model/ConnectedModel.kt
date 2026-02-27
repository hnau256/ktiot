@file:UseSerializers(
    MutableStateFlowSerializer::class,
)

package hnau.ktiot.client.model

import org.hnau.commons.kotlin.getOrInit
import org.hnau.commons.kotlin.serialization.MutableStateFlowSerializer
import org.hnau.commons.kotlin.toAccessor
import org.hnau.commons.app.model.goback.GoBackHandler
import hnau.ktiot.client.model.screen.ScreenModel
import hnau.ktiot.scheme.topic.MqttTopic
import org.hnau.commons.gen.pipe.annotations.Pipe
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