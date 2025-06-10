package hnau.ktiot.client.model.property

import arrow.core.None
import arrow.core.toOption
import hnau.common.kotlin.Loadable
import hnau.common.kotlin.Loading
import hnau.common.kotlin.Ready
import hnau.common.kotlin.coroutines.Stickable
import hnau.common.kotlin.coroutines.combineState
import hnau.common.kotlin.coroutines.flatMapState
import hnau.common.kotlin.coroutines.mapState
import hnau.common.kotlin.coroutines.operationOrNullIfExecuting
import hnau.common.kotlin.coroutines.predeterminated
import hnau.common.kotlin.coroutines.stateFlow
import hnau.common.kotlin.coroutines.stick
import hnau.common.kotlin.fold
import hnau.common.kotlin.getOrInit
import hnau.common.kotlin.shrinkType
import hnau.common.kotlin.toAccessor
import hnau.common.logging.tryOrLog
import hnau.common.model.goback.GoBackHandler
import hnau.common.model.goback.NeverGoBackHandler
import hnau.common.mqtt.utils.MqttClient
import hnau.ktiot.client.model.utils.Timestamped
import hnau.ktiot.scheme.Element
import hnau.ktiot.scheme.PropertyMode
import hnau.ktiot.scheme.PropertyType
import hnau.ktiot.scheme.topic.ChildTopic
import hnau.ktiot.scheme.topic.raw
import hnau.pipe.annotations.Pipe
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {  }

class PropertyModel(
    scope: CoroutineScope,
    private val dependencies: Dependencies,
    skeleton: Skeleton,
    val topic: ChildTopic,
    private val property: Element.Property<*>
) {

    @Pipe
    interface Dependencies {

        val mqttClient: MqttClient

        fun fraction(): FractionModel.Dependencies
    }

    @Serializable
    data class Skeleton(
        var value: ValueModel.Skeleton? = null,
    )

    val mode: PropertyMode
        get() = property.mode

    val value: StateFlow<Loadable<Result<ValueModel>>> = when (val type = property.type) {
        is PropertyType.Events -> TODO()
        is PropertyType.State -> when (type) {
            is PropertyType.State.Fraction -> dependencies
                .mqttClient
                .let { client ->
                    client
                        .subscribe(
                            topic = topic.topic.raw,
                        )
                        .map { element ->
                            logger.tryOrLog(
                                log = "parsing '$element' from $topic",
                                block = {
                                    Json.decodeFromJsonElement(
                                        deserializer = type.serializer,
                                        element = element
                                    )
                                    //TODO(error bubble) if exception
                                }
                            ).let(::Ready)
                        }
                        .stateIn(
                            scope = scope,
                            started = SharingStarted.Eagerly,
                            initialValue = Loading,
                        )
                        .stick(scope) { stickableScope, valueOrErrorOrLoading ->
                            valueOrErrorOrLoading.fold(
                                ifLoading = { Stickable.predeterminated(Loading) },
                                ifReady = { valueOrError ->
                                    valueOrError.fold(
                                        onSuccess = { initialValue ->
                                            Stickable.stateFlow<_, Float, _>(
                                                initial = initialValue,
                                                tryUseNext = { valueOrErrorOrLoading ->
                                                    valueOrErrorOrLoading.fold(
                                                        ifLoading = { None },
                                                        ifReady = { valueOrError ->
                                                            valueOrError.getOrNull()
                                                                .toOption()
                                                        }
                                                    )
                                                },
                                                createResult = { values ->

                                                    val overwriteValue =
                                                        MutableStateFlow<Timestamped<Float>?>(
                                                            null
                                                        )

                                                    val valuesOrOverwritten = combineState(
                                                        scope = scope,
                                                        a = overwriteValue.mapState(
                                                            scope = stickableScope
                                                        ) { overwrittenOrNull ->
                                                            overwrittenOrNull?.takeIf { overwritten ->
                                                                Clock.System.now() - overwritten.timestamp < 3.seconds
                                                            }
                                                        },
                                                        b = values.mapState(
                                                            scope = stickableScope,
                                                            transform = Timestamped.Companion::now,
                                                        ),
                                                    ) { overwritten, received ->
                                                        when {
                                                            overwritten == null -> received.value to false
                                                            overwritten.timestamp > received.timestamp -> overwritten.value to true
                                                            else -> received.value to false
                                                        }
                                                    }

                                                    FractionModel(
                                                        scope = stickableScope,
                                                        dependencies = dependencies.fraction(),
                                                        skeleton = skeleton::value
                                                            .toAccessor()
                                                            .shrinkType<_, FractionModel.Skeleton>()
                                                            .getOrInit { FractionModel.Skeleton() },
                                                        value = valuesOrOverwritten.mapState(
                                                            stickableScope
                                                        ) { it.first },
                                                        type = type,
                                                        mutable = when (property.mode) {
                                                            PropertyMode.Manual -> true
                                                            PropertyMode.Hardware, PropertyMode.Calculated -> false
                                                        },
                                                        publish = operationOrNullIfExecuting(
                                                            stickableScope
                                                        ) { valueToSend ->
                                                            val encoded = logger
                                                                .tryOrLog(
                                                                    log = "encoding '$valueToSend' for $topic"
                                                                ) {
                                                                    Json.Default.encodeToJsonElement(
                                                                        serializer = type.serializer,
                                                                        value = valueToSend
                                                                    )
                                                                }
                                                                .getOrNull()
                                                                ?: return@operationOrNullIfExecuting //TODO(error bubble)

                                                            overwriteValue.value =
                                                                Timestamped.now(valueToSend)

                                                            client.publish(
                                                                topic = topic.topic.raw,
                                                                value = encoded,
                                                                retained = true,
                                                            ) //TODO(error bubble) if false

                                                            valuesOrOverwritten.first {
                                                                val overwritten = it.second
                                                                !overwritten
                                                            }
                                                        }

                                                    )
                                                        .let(Result.Companion::success)
                                                        .let(::Ready)
                                                }
                                            )
                                        },
                                        onFailure = { error ->
                                            Stickable.predeterminated(
                                                Ready(Result.failure(error))
                                            )
                                        }
                                    )
                                }
                            )
                        }

                }

            is PropertyType.State.Enum -> TODO()
            PropertyType.State.Flag -> TODO()
            is PropertyType.State.Number -> TODO()
            PropertyType.State.RGB -> TODO()
            PropertyType.State.Text -> TODO()
            PropertyType.State.Timestamp -> TODO()
        }
    }

    val goBackHandler: GoBackHandler = value.flatMapState(scope) { valueOrErrorLoading ->
        valueOrErrorLoading.fold(
            ifLoading = { NeverGoBackHandler },
            ifReady = { valueOrError ->
                valueOrError.fold(
                    onFailure = { NeverGoBackHandler },
                    onSuccess = ValueModel::goBackHandler,
                )
            }
        )
    }
}