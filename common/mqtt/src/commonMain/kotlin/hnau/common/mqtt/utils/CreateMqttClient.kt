package hnau.common.mqtt.utils

import hnau.common.mqtt.types.BrokerConfig

internal expect fun createMqttClient(
    config: BrokerConfig,
): MqttClient