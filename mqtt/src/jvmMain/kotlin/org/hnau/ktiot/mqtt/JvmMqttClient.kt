package org.hnau.ktiot.mqtt

import co.touchlab.kermit.Logger
import org.hnau.ktiot.mqtt.platform.MqttClient
import org.hnau.ktiot.mqtt.platform.MqttSimpleSession
import org.hnau.ktiot.mqtt.platform.disconnectFastAndSafe
import org.hnau.ktiot.mqtt.platform.doAsync
import org.hnau.ktiot.mqtt.platform.serverUri
import org.hnau.ktiot.mqtt.platform.toConnectOptions
import org.hnau.ktiot.mqtt.platform.toMqttResult
import org.hnau.ktiot.mqtt.types.BrokerConfig
import org.hnau.ktiot.mqtt.types.MqttResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

internal class JvmMqttClient(
    private val config: BrokerConfig,
) : MqttClient {

    private val logger: Logger = Logger.withTag("JvmMqttClient")

    override suspend fun connect(
        block: suspend (session: MqttSimpleSession) -> Nothing,
    ): MqttResult.Error {

        val uri = config.connection.serverUri
        val client = MqttAsyncClient(
            uri,
            config.connection.clientId,
            MemoryPersistence(),
        )

        val connectionResult = client
            .doAsync {
                connect(config.connection.toConnectOptions())
            }
            .toMqttResult()

        when (connectionResult) {
            is MqttResult.Error -> return connectionResult
            is MqttResult.Success -> Unit
        }

        return try {
            runSession(
                client = client,
                block = block,
            )
        } finally {
            client.disconnectFastAndSafe(
                logger = logger,
            )
        }
    }

    private suspend fun runSession(
        client: MqttAsyncClient,
        block: suspend (session: MqttSimpleSession) -> Nothing,
    ): MqttResult.Error = coroutineScope {

        val scope: CoroutineScope = this

        val result: CompletableDeferred<MqttResult.Error> = CompletableDeferred()

        val session = JvmMqttSimpleSession(
            client = client,
            messagesBufferSize = config.messagesBufferSize,
            onDisconnected = result::complete
        )

        scope.launch { block(session) }

        result.await()
    }
}