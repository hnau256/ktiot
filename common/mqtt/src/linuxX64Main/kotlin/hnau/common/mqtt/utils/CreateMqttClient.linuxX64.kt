package hnau.common.mqtt.utils

import hnau.common.mqtt.types.BrokerConfig

internal actual fun createMqttClient(
    config: BrokerConfig,
): MqttClient {
    throw NotImplementedError()
}