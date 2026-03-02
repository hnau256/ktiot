package org.hnau.commons.mqtt.utils

import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.hnau.commons.mqtt.MqttConfig

internal val MqttConfig.serverUri: String
    get() = "${protocol.uriScheme}://$host:$port"

private val MqttConfig.Protocol.uriScheme: String
    get() =
        when (this) {
            MqttConfig.Protocol.TCP -> "tcp"
            MqttConfig.Protocol.SSL -> "ssl"
        }

internal fun MqttConfig.toConnectOptions() =
    MqttConnectOptions().apply {
        val auth = auth ?: return@apply
        userName = auth.username
        password = auth.password.toCharArray()
    }
