package hnau.impl

import hnau.common.mqtt.utils.MqttConfig
import hnau.ktiot.coordinator.coordinator
import hnau.ktiot.scheme.PropertyMode
import hnau.ktiot.scheme.PropertyType
import hnau.ktiot.scheme.topic.MqttTopic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.simple.SimpleLogger

fun main() = runBlocking {
    System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE")
    coordinator(
        config = MqttConfig(
            address = "192.168.0.11",
            clientId = "coordinator",
            auth = MqttConfig.Auth(
                user = "coordinator",
                password = "qwerty",
            )
        ),
        builds = MutableStateFlow { scope ->
            val master = property(
                topic = MqttTopic.Relative("master"),
                type = PropertyType.State.Text,
                publishMode = PropertyMode.Manual,
            )
            val slave = property(
                topic = MqttTopic.Relative("slave"),
                type = PropertyType.State.Text,
                publishMode = PropertyMode.Calculated,
            )
            scope.launch {
                master.publish("QWERTY", true)
                master.subscribe().collect {
                    slave.publish(it, true)
                }
            }
        }
    )
}