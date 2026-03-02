package hnau.common.mqtt.utils

import org.eclipse.paho.client.mqttv3.MqttMessage

internal fun MqttMessage.toMessage() =
    Message(
        id = id,
        payload = payload,
        retained = isRetained,
    )
