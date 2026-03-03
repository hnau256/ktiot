package hnau.common.mqtt.platform

import hnau.common.mqtt.types.BrokerConfig

internal expect fun createMqttClient(
    config: BrokerConfig,
): MqttClient