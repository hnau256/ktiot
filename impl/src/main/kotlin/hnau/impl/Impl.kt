package hnau.impl

import hnau.common.mqtt.utils.MqttConfig
import hnau.ktiot.coordinator.ScreenBuilder
import hnau.ktiot.coordinator.coordinator
import hnau.ktiot.coordinator.utils.typed
import hnau.ktiot.scheme.PropertyMode
import hnau.ktiot.scheme.PropertyType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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

            val events = this
                .property("events")
                .typed(PropertyType.State.Flag)
                .fallback { false }
                .share(PropertyMode.Hardware)

            val value = this
                .property("value")
                .typed(PropertyType.State.Fraction())
                .share(PropertyMode.Calculated)

            value.bind(
                values = events
                    .subscribe()
                    .map {
                        when (it) {
                            false -> 0f
                            true -> 1f
                        }
                    },
                retained = true,
            )

            /*addType(
                prefix = "text",
                type = PropertyType.State.Text,
                initialValue = "QWERTY"
            )
            addType(
                prefix = "fraction",
                type = PropertyType.State.Fraction(
                    range = 0f..10f
                ),
                initialValue = 3.5f,
            )
            addType(
                prefix = "flag",
                type = PropertyType.State.Flag,
                initialValue = true,
            )*/
        }
    )
}

private fun <T> ScreenBuilder.addType(
    prefix: String,
    type: PropertyType.State<T>,
    initialValue: T,
) {
    val master = this
        .property("${prefix}_master")
        .typed(type)
        .fallback { initialValue }
        .share(PropertyMode.Manual)

    val slave = this
        .property("${prefix}_slave")
        .typed(type)
        .share(PropertyMode.Calculated)

    slave.bind(
        values = master.subscribe(),
        retained = true,
    )
}