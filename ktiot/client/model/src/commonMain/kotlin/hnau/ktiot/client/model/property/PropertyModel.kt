package hnau.ktiot.client.model.property

import hnau.common.model.goback.GoBackHandler
import hnau.common.model.goback.NeverGoBackHandler
import hnau.common.mqtt.utils.MqttClient
import hnau.ktiot.scheme.Element
import hnau.ktiot.scheme.topic.MqttTopic
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable

class PropertyModel(
    scope: CoroutineScope,
    private val dependencies: Dependencies,
    skeleton: Skeleton,
) {

    @Pipe
    interface Dependencies {

        val topic: MqttTopic.Absolute

        val mqttClient: MqttClient

        val property: Element.Property<*>
    }

    @Serializable
    /*data*/ class Skeleton

    val topic: MqttTopic.Absolute
        get() = dependencies.topic

    val goBackHandler: GoBackHandler = NeverGoBackHandler
}