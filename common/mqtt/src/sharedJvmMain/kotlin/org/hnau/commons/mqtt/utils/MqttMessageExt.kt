package org.hnau.commons.mqtt.utils

import org.eclipse.paho.client.mqttv3.MqttMessage
import org.hnau.commons.mqtt.Message

internal fun MqttMessage.toMessage() =
    Message(
        id = id,
        payload = payload,
        retained = isRetained,
    )
