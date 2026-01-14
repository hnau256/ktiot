package hnau.ktiot.coordinator.client

import hnau.common.mqtt.utils.MqttClient
import hnau.ktiot.coordinator.ElementWithChildren
import hnau.ktiot.coordinator.utils.Property
import hnau.ktiot.scheme.PropertyType
import hnau.ktiot.scheme.topic.MqttTopic

interface RelativeMqttClient {

    fun <T, P : PropertyType<T>> property(
        topic: MqttTopic.Relative,
        type: P,
    ): Property<T, P>

    fun child(
        topic: MqttTopic.Relative,
    ): RelativeMqttClient

    fun <T: ElementWithChildren.Type> element(
        topic: MqttTopic.Relative,
        block: (RelativeMqttClient) -> T,
    ): ElementWithChildren<T>

    companion object {

        fun createRoot(
            client: MqttClient,
        ): RelativeMqttClient = RelativeMqttClientImpl(
            client = client,
            topic = MqttTopic.Absolute.root,
        )
    }
}