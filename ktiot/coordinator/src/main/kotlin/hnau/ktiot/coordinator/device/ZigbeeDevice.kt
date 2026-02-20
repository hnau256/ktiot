package hnau.ktiot.coordinator.device

import hnau.common.kotlin.Loadable
import hnau.common.kotlin.coroutines.flow.state.mapState
import hnau.common.kotlin.fold
import hnau.common.kotlin.foldBoolean
import hnau.common.kotlin.map
import hnau.common.mqtt.utils.MqttClient
import hnau.ktiot.coordinator.ext.combineLoadableStateWith
import hnau.ktiot.coordinator.utils.subscribeJson
import hnau.ktiot.scheme.topic.MqttTopic
import hnau.ktiot.scheme.topic.raw
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


data class ZigbeeDevice<S>(
    val device: Device,
    val state: StateFlow<Loadable<S>>,
    val client: MqttClient,
    val topic: MqttTopic.Absolute,
) {

    suspend fun set(
        field: String,
        value: String,
    ) {
        client.publish(
            topic = (topic + "set" + field).raw,
            payload = value.toByteArray(Charsets.UTF_8),
            retained = true,
        )
    }

    fun set(
        scope: CoroutineScope,
        field: String,
        value: StateFlow<Loadable<String>>,
    ) {
        scope.launch {
            value.collectLatest { valueOrLoading ->
                valueOrLoading.fold(
                    ifLoading = {},
                    ifReady = { currentValue ->
                        set(
                            field = field,
                            value = currentValue,
                        )
                    }
                )
            }
        }
    }

    companion object {

        const val linkQualityFieldName: String = "linkquality"

        fun <S> create(
            id: ZigbeeDeviceId,
            scope: CoroutineScope,
            client: MqttClient,
            stateSerializer: KSerializer<S>,
            extractLinkQuality: (S) -> Int,
            extractBattery: ((S) -> Float)? = null,
        ): ZigbeeDevice<S> {

            val topic = Zigbee2Mqtt.root + id.id

            val state = client
                .subscribeJson(
                    scope = scope,
                    topic = topic.raw,
                    deserializer = stateSerializer,
                    typeDescription = "Zigbee device state",
                )

            val isOnline: StateFlow<Loadable<Boolean>> = client
                .subscribeJson(
                    scope = scope,
                    topic = (topic + "availability").raw,
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

            return ZigbeeDevice(
                state = state,
                topic = topic,
                client = client,
                device = Device(
                    name = id.id,
                    state = isOnline.combineLoadableStateWith(
                        scope = scope,
                        other = state,
                    ) { isOnline, state ->
                        isOnline.foldBoolean(
                            ifFalse = { Device.State.Offline },
                            ifTrue = {
                                Device.State.Online(
                                    battery = extractBattery?.invoke(state),
                                    connectionQuality = extractLinkQuality(state).toFloat() / 255f,
                                )
                            }
                        )
                    }
                )
            )
        }

        @Serializable
        data class WriteOnlyState(
            @SerialName(linkQualityFieldName)
            val linkQuality: Int,
        )

        fun createWriteOnly(
            scope: CoroutineScope,
            client: MqttClient,
            id: ZigbeeDeviceId,
        ): ZigbeeDevice<WriteOnlyState> = create(
            id = id,
            scope = scope,
            client = client,
            stateSerializer = WriteOnlyState.serializer(),
            extractLinkQuality = WriteOnlyState::linkQuality,
            extractBattery = null, //TODO
        )
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