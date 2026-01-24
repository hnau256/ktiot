package hnau.impl

import hnau.common.mqtt.utils.MqttConfig
import hnau.ktiot.coordinator.asReadyStateFlow
import hnau.ktiot.coordinator.coordinator
import hnau.ktiot.scheme.topic.MqttTopic
import kotlinx.coroutines.runBlocking
import org.slf4j.simple.SimpleLogger

fun main() = runBlocking {
    System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE")

    coordinator(
        config = MqttConfig(
            address = "192.168.0.13",
            clientId = "coordinator",
        )
    ) { scope, client ->
        createHome(
            scope = scope,
            topic = MqttTopic.Absolute.root,
            client = client,
        ).asReadyStateFlow()
    }
}