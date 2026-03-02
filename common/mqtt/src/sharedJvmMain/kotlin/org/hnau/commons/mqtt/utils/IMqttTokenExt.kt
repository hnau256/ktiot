package org.hnau.commons.mqtt.utils

import kotlinx.coroutines.suspendCancellableCoroutine
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import kotlin.coroutines.resume

internal suspend fun IMqttToken.await(): Result<Unit> =
    suspendCancellableCoroutine { continuation ->
        setActionCallback(
            object : IMqttActionListener {
                override fun onSuccess(token: IMqttToken) {
                    continuation.resume(Result.success(Unit))
                }

                override fun onFailure(
                    token: IMqttToken,
                    cause: Throwable,
                ) {
                    continuation.resume(Result.failure(cause))
                }
            },
        )
    }
