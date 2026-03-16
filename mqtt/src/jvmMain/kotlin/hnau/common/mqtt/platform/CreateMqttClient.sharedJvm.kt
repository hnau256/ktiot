package hnau.common.mqtt.platform

import hnau.common.mqtt.JvmMqttClient
import hnau.common.mqtt.types.BrokerConfig

internal actual fun createMqttClient(
    config: BrokerConfig,
): MqttClient = JvmMqttClient(
    config = config,
)