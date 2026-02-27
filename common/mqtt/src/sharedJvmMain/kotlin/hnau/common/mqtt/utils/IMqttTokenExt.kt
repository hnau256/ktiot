package org.hnau.commons.mqtt.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.IMqttToken

internal suspend fun IMqttToken.await(): Unit = withContext(Dispatchers.IO) {
    waitForCompletion()
    exception?.let { throw it }
}