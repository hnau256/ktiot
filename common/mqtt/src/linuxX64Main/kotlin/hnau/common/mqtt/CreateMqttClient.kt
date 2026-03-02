package hnau.common.mqtt.internal

import hnau.common.mqtt.utils.MqttConfig

internal actual fun createMqttClient(config: MqttConfig): MqttClient = throw NotImplementedError()
