package hnau.ktiot.coordinator.device.zigbee

import hnau.common.mqtt.types.MqttSession
import hnau.common.mqtt.types.topic.Topic
import hnau.ktiot.coordinator.AlertRegistry
import hnau.ktiot.coordinator.device.Device
import hnau.ktiot.coordinator.device.DeviceRegistry
import hnau.ktiot.coordinator.ext.combineLoadableStateWith
import hnau.ktiot.coordinator.utils.Timestamped
import hnau.ktiot.coordinator.utils.subscribeJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.hnau.commons.gen.pipe.annotations.Pipe
import org.hnau.commons.kotlin.*
import org.hnau.commons.kotlin.coroutines.flow.state.mapState
import org.hnau.commons.kotlin.logging.tryOrLog
import kotlin.Boolean
import kotlin.Result
import kotlin.String
import kotlin.div
import kotlin.fold
import kotlin.let
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.toString


data class ZigBeeDevice(
    private val id: ZigBeeDeviceId,
    private val scope: CoroutineScope,
    private val dependencies: Dependencies,
) {

    @Pipe
    interface Dependencies {

        val client: MqttSession

        val alertRegistry: AlertRegistry

        val deviceRegistry: DeviceRegistry
    }

    private val topic: Topic.Absolute = z2mTopic + id.id

    private val stateFieldsOrError: StateFlow<Loadable<Result<JsonObject>>> = dependencies
        .client
        .subscribe(
            topic = topic,
        )
        .map { message ->
            val json = message
                .payload
                .decodeToString()
            logger
                .tryOrLog(
                    log = "Parse zigbee device ${id.id} state from '$json'"
                ) {
                    Json.parseToJsonElement(json).castOrThrow<JsonObject>()
                }
                .let(::Ready)
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = Loading,
        )

    private val stateFields: StateFlow<Loadable<JsonObject>> =
        stateFieldsOrError.mapState(scope) { stateFieldsOrLoading ->
            stateFieldsOrLoading.flatMap { stateFieldsOrError ->
                stateFieldsOrError.fold(
                    onSuccess = ::Ready,
                    onFailure = { Loading },
                )
            }
        }

    val isOnline: StateFlow<Loadable<Boolean>> = dependencies
        .client
        .subscribeJson(
            scope = scope,
            topic = (topic + "availability"),
            deserializer = Availability.serializer(),
            typeDescription = "Zigbee device availability",
        )
        .mapState(scope) { availabilityOrLoading ->
            availabilityOrLoading.map { availability ->
                when (availability.state) {
                    Availability.State.Offline -> false
                    Availability.State.Online -> true
                }
            }
        }

    init {

        scope.launch {
            stateFieldsOrError.collectLatest { fieldsOrLoading ->
                fieldsOrLoading.fold(
                    ifLoading = {},
                    ifReady = { fieldsOrError ->
                        fieldsOrError.fold(
                            onSuccess = {},
                            onFailure = { error ->
                                dependencies
                                    .alertRegistry
                                    .registerAlert("Unable read fields from device:${id.id}. Error: ${error.message}")
                            }
                        )
                    }
                )
            }
        }

        scope.launch {
            dependencies
                .deviceRegistry
                .registerDevice(
                    Device(
                        name = id.id,
                        state = isOnline.combineLoadableStateWith(
                            scope = scope,
                            other = stateFields
                        ) { isOnline, stateFields ->
                            isOnline.foldBoolean(
                                ifFalse = { Device.State.Offline },
                                ifTrue = {
                                    Device.State.Online(
                                        connectionQuality = stateFields["linkquality"]?.jsonPrimitive?.float?.div(255f),
                                        battery = stateFields["battery"]?.jsonPrimitive?.float?.div(100f),
                                    )
                                }
                            )
                        }
                    )
                )
        }
    }

    fun publish(
        field: String,
        value: StateFlow<Loadable<String>>,
    ) {

        val actual: StateFlow<Loadable<Timestamped<String>>> =
            subscribe(field).mapState(scope) { valueOrLoading ->
                valueOrLoading.map { value ->
                    Timestamped(
                        value = value,
                        timestamp = Clock.System.now(),
                    )
                }
            }

        val target: StateFlow<Loadable<Timestamped<String>>> = value.mapState(scope) { valueOrLoading ->
            valueOrLoading.map { value ->
                Timestamped(
                    value = value,
                    timestamp = Clock.System.now(),
                )
            }
        }

        val actualWithTarget: StateFlow<Loadable<ActualWithTarget>> = actual.combineLoadableStateWith(
            scope = scope,
            other = target,
        ) { (actual, actualTimestamp), (target, targetTimestamp) ->
            ActualWithTarget(
                actual = actual,
                target = target,
                targetIsNewer = targetTimestamp > actualTimestamp
            )
        }

        scope.launch {
            actualWithTarget.collectLatest { actualWithTargetOrLoading ->
                actualWithTargetOrLoading.fold(
                    ifLoading = {},
                    ifReady = { (actual, target, targetIsNewer) ->
                        if (actual == target) {
                            return@fold
                        }
                        if (!targetIsNewer) {
                            delay(actualIsChangedDebounce)
                        }
                        var retryDelay = publishRetryInitialDelay
                        while (true) {
                            dependencies
                                .client
                                .publish(
                                    topic = (topic + "set" + field),
                                    retained = false,
                                    payload = target.toByteArray(Charsets.UTF_8)
                                )
                            delay(retryDelay)
                            retryDelay = minOf(retryDelay * 2, publishRetryMaxDelay)
                        }
                    }
                )
            }
        }

        scope.launch {
            actualWithTarget
                .mapState(scope) { actualWithTargetOrLoading ->
                    actualWithTargetOrLoading.fold(
                        ifLoading = { true },
                        ifReady = { (actual, target) -> actual == target }
                    )
                }
                .collectLatest { actualIsCorrect ->
                    if (actualIsCorrect) {
                        return@collectLatest
                    }
                    delay(actualIsIncorrectDebounce)
                    dependencies
                        .alertRegistry
                        .registerAlert("Device:${id.id}, field:$field is in incorrect state")
                }
        }
    }

    private data class ActualWithTarget(
        val actual: String,
        val target: String,
        val targetIsNewer: Boolean,
    )

    fun subscribe(
        field: String,
    ): StateFlow<Loadable<String>> = stateFields.mapState(scope) { stateFieldsOrLoading ->
        stateFieldsOrLoading.flatMap { stateFields ->
            stateFields[field].foldNullable(
                ifNull = { Loading },
                ifNotNull = { it.jsonPrimitive.toString().let(::Ready) }
            )
        }
    }

    companion object {

        private val z2mTopic: Topic.Absolute = Topic.Absolute.root + "zigbee2mqtt"

        private val actualIsChangedDebounce: Duration = 1.seconds

        private val publishRetryInitialDelay: Duration = 5.seconds

        private val publishRetryMaxDelay: Duration = 1.hours

        private val actualIsIncorrectDebounce: Duration = 3.minutes
    }
}

@Serializable
private data class Availability(
    @SerialName("state")
    val state: State,
) {

    enum class State {
        @SerialName("offline")
        Offline,

        @SerialName("online")
        Online,
    }
}