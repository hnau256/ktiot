package hnau.common.mqtt

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import hnau.common.mqtt.internal.MqttClient
import hnau.common.mqtt.internal.MqttResult
import hnau.common.mqtt.internal.MqttSession
import hnau.common.mqtt.utils.await
import hnau.common.mqtt.utils.serverUri
import hnau.common.mqtt.utils.toConnectOptions
import hnau.common.mqtt.utils.toDisconnected
import hnau.common.mqtt.utils.toUnableToConnect

internal class JvmMqttClient(
    private val mqttConfig: MqttConfig,
) : MqttClient {
    override suspend fun connect(
        config: MqttBrokerConfig,
        block: suspend MqttSession.() -> Nothing,
    ): MqttResult {
        val pahoClient =
            MqttAsyncClient(
                config.serverUri,
                config.clientId,
                MemoryPersistence(),
            )
        val connectionError = pahoClient.establishConnection(config)
        if (connectionError != null) return connectionError

        return try {
            runSession(pahoClient, block)
        } finally {
            pahoClient.disconnect().await()
        }
    }

    private suspend fun MqttAsyncClient.establishConnection(config: MqttBrokerConfig): MqttResult.UnableToConnect? =
        connect(config.toConnectOptions())
            .await()
            .exceptionOrNull()
            ?.let { e ->
                if (e is MqttException) {
                    e.toUnableToConnect()
                } else {
                    MqttResult.UnableToConnect.NetworkError(cause = e)
                }
            }

    private suspend fun runSession(
        pahoClient: MqttAsyncClient,
        block: suspend MqttSession.() -> Nothing,
    ): MqttResult.Disconnected {
        val session =
            JvmMqttSession(
                pahoClient = pahoClient,
                mqttConfig = mqttConfig,
            )
        return try {
            session.awaitBlock(block)
        } catch (e: BrokerDisconnectedException) {
            val cause = e.cause
            when {
                cause is MqttException -> cause.toDisconnected()
                else -> MqttResult.Disconnected.BrokerDisconnected(cause = cause)
            }
        } catch (e: MqttException) {
            e.toDisconnected()
        }
    }

    private suspend fun JvmMqttSession.awaitBlock(block: suspend MqttSession.() -> Nothing): Nothing =
        coroutineScope {
            val blockJob = async { block(this@awaitBlock) }
            val disconnectJob =
                async {
                    val cause = disconnectDeferred.await()
                    blockJob.cancel(BrokerDisconnectedException(cause))
                }
            try {
                blockJob.await()
            } finally {
                disconnectJob.cancel()
                close()
            }
        }
}

private class BrokerDisconnectedException(
    cause: Throwable?,
) : CancellationException(cause?.message) {
    init {
        initCause(cause)
    }
}
