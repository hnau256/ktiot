package hnau.ktiot.client.model.utils

import hnau.ktiot.scheme.topic.MqttTopic

sealed interface ChildTopic {

    val topic: MqttTopic.Absolute

    data class Relative(
        val parent: MqttTopic.Absolute,
        val child: MqttTopic.Relative,
    ): ChildTopic {

        override val topic: MqttTopic.Absolute =
            parent + child
    }

    data class Absolute(
        override val topic: MqttTopic.Absolute
    ): ChildTopic

    companion object
}