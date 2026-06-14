package org.hnau.ktiot.mqtt.platform

import org.hnau.ktiot.mqtt.JvmMqttClient
import org.hnau.ktiot.mqtt.types.BrokerConfig

internal actual fun createMqttClient(
    config: BrokerConfig,
): MqttClient = JvmMqttClient(
    config = config,
)