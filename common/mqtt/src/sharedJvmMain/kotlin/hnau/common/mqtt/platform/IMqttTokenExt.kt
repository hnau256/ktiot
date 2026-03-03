package hnau.common.mqtt.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttException
import org.hnau.commons.kotlin.foldNullable

internal suspend fun IMqttToken.await(): Result<Unit> = withContext(Dispatchers.IO) {
    waitForCompletion()
    val nullableError: MqttException? = exception
    return@withContext nullableError.foldNullable(
        ifNull = { Result.success(Unit) },
        ifNotNull = { error -> Result.failure(error) }
    )
}