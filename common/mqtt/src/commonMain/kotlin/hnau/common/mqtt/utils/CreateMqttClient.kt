package hnau.common.mqtt.utils

import kotlinx.coroutines.CoroutineScope

expect suspend fun createMqttClient(
    scope: CoroutineScope,
    config: MqttConfig,
): Result<MqttClient>