package hnau.common.mqtt.utils

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttException
import hnau.common.mqtt.internal.MqttOperationError

internal data class PahoOperation(
    val type: Type,
    val result: kotlinx.coroutines.CompletableDeferred<Either<MqttOperationError, Unit>>,
) {
    sealed interface Type {
        suspend fun execute(client: IMqttAsyncClient): Either<MqttOperationError, Unit>

        data class Subscribe(
            val topic: String,
            val qoS: Int,
        ) : Type {
            override suspend fun execute(client: IMqttAsyncClient) =
                pahoOperation(
                    block = { client.subscribe(topic, qoS).await() },
                    mapError = { e ->
                        MqttOperationError(
                            cause = e,
                            type =
                                when {
                                    e is MqttException && e.reasonCode == MqttException.REASON_CODE_SUBSCRIBE_FAILED.toInt() ->
                                        MqttOperationError.Type.BrokerRefused
                                    else -> MqttOperationError.Type.NetworkError
                                },
                        )
                    },
                )
        }

        data class Unsubscribe(
            val topic: String,
        ) : Type {
            override suspend fun execute(client: IMqttAsyncClient) =
                pahoOperation(
                    block = { client.unsubscribe(topic).await() },
                    mapError = { e -> MqttOperationError(cause = e, type = MqttOperationError.Type.NetworkError) },
                )
        }

        data class Publish(
            val topic: String,
            val payload: ByteArray,
            val qoS: Int,
            val retained: Boolean,
        ) : Type {
            override suspend fun execute(client: IMqttAsyncClient) =
                pahoOperation(
                    block = { client.publish(topic, payload, qoS, retained).await() },
                    mapError = { e -> MqttOperationError(cause = e, type = MqttOperationError.Type.NetworkError) },
                )
        }
    }
}

private suspend fun pahoOperation(
    block: suspend () -> Result<Unit>,
    mapError: (Throwable) -> MqttOperationError,
): Either<MqttOperationError, Unit> =
    block().fold(
        onSuccess = { Unit.right() },
        onFailure = { mapError(it).left() },
    )
