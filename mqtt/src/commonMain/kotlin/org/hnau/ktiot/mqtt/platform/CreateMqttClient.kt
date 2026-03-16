package org.hnau.ktiot.mqtt.platform

import org.hnau.ktiot.mqtt.types.BrokerConfig

internal expect fun createMqttClient(
    config: BrokerConfig,
): MqttClient