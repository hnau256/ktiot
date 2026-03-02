package org.hnau.commons.mqtt.internal

import org.hnau.commons.mqtt.MqttConfig

internal expect fun createMqttClient(config: MqttConfig): MqttClient
