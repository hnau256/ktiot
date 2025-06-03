@file:UseSerializers(
    MutableStateFlowSerializer::class,
)

package hnau.ktiot.client.model

import hnau.common.model.goback.GoBackHandler
import hnau.common.model.goback.GoBackHandlerProvider
import hnau.common.kotlin.serialization.MutableStateFlowSerializer
import hnau.common.model.goback.NeverGoBackHandler
import hnau.common.mqtt.utils.MqttClient
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

        val mqttClient: MqttClient
    }

    @Serializable
    /*data*/ class Skeleton

    val goBackHandler: GoBackHandler = NeverGoBackHandler
}