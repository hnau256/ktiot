package hnau.common.mqtt.utils

import org.eclipse.paho.client.mqttv3.MqttMessage
import hnau.common.mqtt.Message

internal fun MqttMessage.toMessage() =
    Message(
        id = id,
        payload = payload,
        retained = isRetained,
    )
