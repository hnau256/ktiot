package hnau.common.mqtt.internal

import hnau.common.mqtt.MqttConfig

internal expect fun createMqttClient(config: MqttConfig): MqttClient
