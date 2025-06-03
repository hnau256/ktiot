package hnau.impl

import hnau.common.mqtt.utils.MqttConfig
import hnau.ktiot.coordinator.coordinator
import hnau.ktiot.scheme.PropertyMode
import hnau.ktiot.scheme.PropertyType
import hnau.ktiot.scheme.topic.MqttTopic
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.slf4j.simple.SimpleLogger

fun main() = runBlocking {
    System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE")
    coordinator(
        config = MqttConfig(
            address = "192.168.0.11",
            clientId = "coordinator",
            auth = MqttConfig.Auth(
                user = "hnau",
                password = "gotazike",
            )
        ),
        builds = MutableStateFlow { scope ->
            val value = property(
                topic = MqttTopic.Relative("value"),
                type = PropertyType.State.Number(
                    suffix = "kg",
                ),
                publishMode = PropertyMode.Calculated,
            )
            scope.launch {
                ticker(
                    delayMillis = 1000,
                    initialDelayMillis = 100,
                ).consumeEach {
                    value.publish(
                        value = Clock.System
                            .now()
                            .toLocalDateTime(TimeZone.currentSystemDefault()).time.toSecondOfDay() / 1000f,
                        retained = true,
                    )
                }
            }
        }
    )
}