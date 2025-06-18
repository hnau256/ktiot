package hnau.common.mqtt.utils

import kotlinx.coroutines.CoroutineScope

actual suspend fun createMqttClient(
    scope: CoroutineScope,
    config: MqttConfig,
): Result<MqttClient> = Result.failure(NotImplementedError())