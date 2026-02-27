package org.hnau.commons.mqtt.utils

import kotlinx.coroutines.CoroutineScope

expect suspend fun createMqttClient(
    scope: CoroutineScope,
    config: MqttConfig,
): Result<MqttClient>