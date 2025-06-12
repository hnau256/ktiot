package hnau.impl

import hnau.common.mqtt.utils.MqttConfig
import hnau.ktiot.coordinator.ScreenBuilder
import hnau.ktiot.coordinator.coordinator
import hnau.ktiot.scheme.PropertyMode
import hnau.ktiot.scheme.PropertyType
import hnau.ktiot.scheme.topic.MqttTopic
import kotlinx.coroutines.CoroutineScope
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
            addType(
                scope = scope,
                prefix = "text",
                type = PropertyType.State.Text,
                initialValue = "QWERTY"
            )
            addType(
                scope = scope,
                prefix = "fraction",
                type = PropertyType.State.Fraction(
                    range = 0f..10f
                ),
                initialValue = 3.5f,
            )
            addType(
                scope = scope,
                prefix = "flag",
                type = PropertyType.State.Flag,
                initialValue = true,
            )
        }
    )
}

private fun <T> ScreenBuilder.addType(
    scope: CoroutineScope,
    prefix: String,
    type: PropertyType.State<T>,
    initialValue: T,
) {
    val master = property(
        topic = MqttTopic.Relative("${prefix}_master"),
        type = type,
        publishMode = PropertyMode.Manual,
    )
    val slave = property(
        topic = MqttTopic.Relative("${prefix}_slave"),
        type = type,
        publishMode = PropertyMode.Calculated,
    )
    scope.launch {
        master.publish(initialValue, true)
        master.subscribe().collect {
            slave.publish(it, true)
        }
    }
}