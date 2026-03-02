package hnau.common.mqtt.internal

import hnau.common.mqtt.MqttConfig

internal actual fun createMqttClient(config: MqttConfig): MqttClient = throw NotImplementedError()
