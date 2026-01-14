package hnau.ktiot.coordinator.client

import hnau.common.mqtt.utils.MqttClient
import hnau.ktiot.coordinator.ElementWithChildren
import hnau.ktiot.coordinator.utils.Property
import hnau.ktiot.coordinator.utils.PropertyImpl
import hnau.ktiot.scheme.PropertyType
import hnau.ktiot.scheme.topic.MqttTopic

class RelativeMqttClientImpl(
    private val client: MqttClient,
    private val topic: MqttTopic.Absolute,
) : RelativeMqttClient {

    override fun <T, P : PropertyType<T>> property(
        topic: MqttTopic.Relative,
        type: P
    ): Property<T, P> = PropertyImpl(
        client = client,
        parentTopic = this.topic,
        topic = topic,
        type = type,
    )

    override fun <T : ElementWithChildren.Type> element(
        topic: MqttTopic.Relative,
        block: (RelativeMqttClient) -> T
    ): ElementWithChildren<T> = ElementWithChildren(
        topic = topic,
        type = block(
            RelativeMqttClientImpl(
                client = client,
                topic = this.topic + topic,
            )
        )
    )
}