package org.hnau.commons.mqtt

expect fun createMqttClient(config: MqttClientConfig = MqttClientConfig()): MqttClient
