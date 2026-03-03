package hnau.common.mqtt.platform

import hnau.common.mqtt.types.BrokerConfig
import org.eclipse.paho.client.mqttv3.MqttConnectOptions

internal val BrokerConfig.Connection.serverUri: String
    get() = "${protocol.uriScheme}://$host:$port"

private val BrokerConfig.Connection.Protocol.uriScheme: String
    get() = when (this) {
        BrokerConfig.Connection.Protocol.TCP -> "tcp"
        BrokerConfig.Connection.Protocol.SSL -> "ssl"
    }

internal fun BrokerConfig.Connection.toConnectOptions(): MqttConnectOptions =
    MqttConnectOptions().apply {
        auth?.let { auth ->
            userName = auth.username
            password = auth.password.toCharArray()
        }
    }

