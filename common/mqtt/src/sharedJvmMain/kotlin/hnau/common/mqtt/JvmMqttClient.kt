package hnau.common.mqtt

import co.touchlab.kermit.Logger
import hnau.common.mqtt.types.BrokerConfig
import hnau.common.mqtt.platform.MqttClient
import hnau.common.mqtt.platform.MqttResult
import hnau.common.mqtt.platform.MqttSession
import hnau.common.mqtt.platform.disconnectFastAndSafe
import hnau.common.mqtt.platform.doAsync
import hnau.common.mqtt.platform.serverUri
import hnau.common.mqtt.platform.toConnectOptions
import hnau.common.mqtt.platform.toMqttResult
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

    override suspend fun <T> connect(
        config: BrokerConfig,
        block: suspend MqttSession.() -> T,
    ): MqttResult<T> {

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

    private suspend fun <R> runSession(
        client: MqttAsyncClient,
        block: suspend MqttSession.() -> R,
    ): MqttResult<R> = coroutineScope {

        val scope: CoroutineScope = this

        val result: CompletableDeferred<MqttResult<R>> = CompletableDeferred()

        val session = JvmMqttSession(
            client = client,
            messagesBufferSize = config.messagesBufferSize,
            onDisconnected = result::complete
        )

        scope.launch {
            val resultValue = session.block()
            val mqttResult = MqttResult.Success(resultValue)
            result.complete(mqttResult)
        }

        result.await()
    }
}