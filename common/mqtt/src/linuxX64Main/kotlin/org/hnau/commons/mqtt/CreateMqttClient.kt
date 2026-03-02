package org.hnau.commons.mqtt.internal

import org.hnau.commons.mqtt.MqttConfig

internal actual fun createMqttClient(config: MqttConfig): MqttClient = throw NotImplementedError()
