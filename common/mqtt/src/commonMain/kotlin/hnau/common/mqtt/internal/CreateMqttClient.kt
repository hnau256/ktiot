package hnau.common.mqtt.internal

import hnau.common.mqtt.utils.MqttConfig

internal expect fun createMqttClient(config: MqttConfig): MqttClient
