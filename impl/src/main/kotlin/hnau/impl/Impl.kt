package hnau.impl

import hnau.common.mqtt.utils.MqttConfig
import hnau.ktiot.coordinator.coordinator
import hnau.ktiot.coordinator.utils.typed
import hnau.ktiot.scheme.PropertyMode
import hnau.ktiot.scheme.PropertyType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock
import org.slf4j.simple.SimpleLogger
import kotlin.math.PI
import kotlin.math.sin

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
        builds = MutableStateFlow {

           child(
                topicPart ="fraction_test",
                builds = MutableStateFlow {

                    val useManual = property("use_manual")
                        .typed(PropertyType.State.Flag)
                        .fallback { false }
                        .share(PropertyMode.Manual)
                        .subscribe()

                    val value = include(
                        topicPart = "manual_config",
                        builds = useManual.map { currentUseManual ->
                            when (currentUseManual) {
                                false -> {
                                    {
                                        property("auto")
                                            .typed(PropertyType.State.Fraction())
                                            .share(PropertyMode.Calculated)
                                            .apply {
                                                bind(
                                                    values = ticker(
                                                        delayMillis = 1000L,
                                                        initialDelayMillis = 0L,
                                                    )
                                                        .consumeAsFlow()
                                                        .map { sin(Clock.System.now().epochSeconds % 10 / 10f * PI.toFloat() * 2) / 2 + 0.5f },
                                                    retained = true,
                                                )
                                            }
                                            .subscribe()
                                    }
                                }

                                true -> {
                                    {
                                        property("manual")
                                            .typed(PropertyType.State.Fraction())
                                            .share(PropertyMode.Manual)
                                            .fallback { 0.25f }
                                            .subscribe()
                                    }
                                }
                            }
                        }
                    )
                        .flatMapLatest { it }

                    property("value")
                        .typed(PropertyType.State.Fraction())
                        .bind(
                            values = value,
                            retained = true,
                        )
                }
            )
        }
    )
}