package org.hnau.ktiot.mqtt.platform

import org.hnau.ktiot.mqtt.types.BrokerConfig
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.hnau.ktiot.mqtt.types.fold

internal val BrokerConfig.Connection.serverUri: String
    get() = "${protocol.uriScheme}://${host.host}:$port"

private val BrokerConfig.Connection.Protocol.uriScheme: String
    get() = fold(
        ifTCP = { "tcp" },
        ifSSL = { "ssl" },
    )

internal fun BrokerConfig.Connection.toConnectOptions(): MqttConnectOptions =
    MqttConnectOptions().apply {
        auth?.let { auth ->
            userName = auth.user
            password = auth.password.toCharArray()
        }
    }

