package hnau.ktiot.coordinator.device

import hnau.ktiot.scheme.topic.MqttTopic

object Zigbee2Mqtt {

    val root: MqttTopic.Absolute = MqttTopic.Absolute.root + "zigbee2mqtt"
}