package hnau.common.mqtt.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.hnau.commons.kotlin.foldBoolean
import org.hnau.commons.kotlin.foldNullable
import kotlin.coroutines.resume

internal suspend fun IMqttToken.await(): Result<Unit> =
    suspendCancellableCoroutine { continuation ->

        setActionCallbackOrCallIfAlreadyCompleted(
            actionCallback = object : IMqttActionListener {
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

@Synchronized
private fun IMqttToken.setActionCallbackOrCallIfAlreadyCompleted(
    actionCallback: IMqttActionListener,
) {
    isComplete.foldBoolean(
        ifTrue = {
            val exceptionOrNull: Exception? = exception
            exceptionOrNull.foldNullable(
                ifNull = { actionCallback.onSuccess(this) },
                ifNotNull = { exception ->
                    actionCallback.onFailure(this, exception)
                }
            )
        },
        ifFalse = { this.actionCallback = actionCallback }
    )
}
