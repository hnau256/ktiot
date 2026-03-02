package hnau.common.mqtt.utils

import org.eclipse.paho.client.mqttv3.MqttConnectOptions

internal val MqttBrokerConfig.serverUri: String
    get() = "${protocol.uriScheme}://$host:$port"

private val MqttBrokerConfig.Protocol.uriScheme: String
    get() =
        when (this) {
            MqttBrokerConfig.Protocol.TCP -> "tcp"
            MqttBrokerConfig.Protocol.SSL -> "ssl"
        }

internal fun MqttBrokerConfig.toConnectOptions() =
    MqttConnectOptions().apply {
        val auth = auth ?: return@apply
        userName = auth.username
        password = auth.password.toCharArray()
    }
