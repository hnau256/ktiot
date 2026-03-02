package org.hnau.commons.mqtt

actual fun createMqttClient(config: MqttClientConfig): MqttClient = JvmMqttClient(config)
