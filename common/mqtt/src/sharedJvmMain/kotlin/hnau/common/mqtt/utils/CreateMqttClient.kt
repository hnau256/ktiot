package org.hnau.commons.mqtt.utils

import kotlinx.coroutines.CoroutineScope
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

actual suspend fun createMqttClient(
    scope: CoroutineScope,
    config: MqttConfig,
): Result<MqttClient> = runCatching {
    MqttAsyncClient(
        with(config) { "${protocol.uriName}://$address:$port" },
        config.clientId,
        MemoryPersistence(),
    )
        .apply {
            connect(
                MqttConnectOptions().apply {
                    config.auth?.let { auth ->
                        userName = auth.user
                        password = auth.password.toCharArray()
                    }
                }
            ).await()
        }
        .let { client ->
            JvmMqttClient(
                scope = scope,
                client = client,
            )
        }
}