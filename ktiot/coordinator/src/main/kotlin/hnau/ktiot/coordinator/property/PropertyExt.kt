package hnau.ktiot.coordinator.property

import org.hnau.commons.kotlin.Loadable
import org.hnau.commons.kotlin.Loading
import org.hnau.commons.kotlin.Ready
import org.hnau.commons.kotlin.coroutines.flow.state.mutable.toMutableStateFlowAsInitial
import org.hnau.commons.kotlin.fold
import org.hnau.commons.logging.tryOrLog
import org.hnau.commons.mqtt.utils.MqttClient
import hnau.ktiot.coordinator.utils.ElementWithChildren
import hnau.ktiot.scheme.PropertyMode
import hnau.ktiot.scheme.PropertyType
import hnau.ktiot.scheme.topic.MqttTopic
import hnau.ktiot.scheme.topic.raw
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger { }

fun <T, P : PropertyType<T>> MqttTopic.Absolute.property(
    type: P,
): PropertyPrototype<T, P> = PropertyPrototype(
    topic = this,
    type = type,
)

fun MqttTopic.Absolute.flagProperty(): PropertyPrototype<Boolean, PropertyType.State.Flag> =
    property(PropertyType.State.Flag)

fun MqttTopic.Absolute.numberProperty(
    suffix: String = "",
    limitMin: Float? = null,
    limitMax: Float? = null,
): PropertyPrototype<Float, PropertyType.State.Number> = property(
    PropertyType.State.Number(
        suffix = suffix,
        limitMin = limitMin,
        limitMax = limitMax,
    ),
)

fun MqttTopic.Absolute.textProperty(): PropertyPrototype<String, PropertyType.State.Text> =
    property(PropertyType.State.Text)

fun MqttTopic.Absolute.ticProperty(): PropertyPrototype<Unit, PropertyType.Events.Tic> =
    property(PropertyType.Events.Tic)

@PublishedApi
internal suspend fun <T, P : PropertyType<T>> Property<T, P, *>.publishPrivate(
    client: MqttClient,
    payload: T,
    retained: Boolean,
): Boolean {
    val payloadBytes = logger
        .tryOrLog(
            log = "encode typed payload to mqtt payload",
        ) {
            Json
                .encodeToString(
                    serializer = type.serializer,
                    value = payload,
                )
                .encodeToByteArray()
        }
        .getOrNull()
        ?: return false
    return client.publish(
        topic = topic.raw,
        payload = payloadBytes,
        retained = retained,
    )
}

@JvmName("publishEvents")
suspend fun <T, P : PropertyType.Events<T>> Property<T, P, Property.Direction.Calculated>.publish(
    client: MqttClient,
    payload: T,
): Boolean = publishPrivate(
    client = client,
    payload = payload,
    retained = false,
)

@JvmName("publishState")
suspend fun <T, P : PropertyType.State<T>> Property<T, P, Property.Direction.Calculated>.publish(
    client: MqttClient,
    payload: T,
): Boolean = publishPrivate(
    client = client,
    payload = payload,
    retained = true,
)

fun <T, P : PropertyType.State<T>> Property<T, P, Property.Direction.Calculated>.publish(
    scope: CoroutineScope,
    client: MqttClient,
    payload: StateFlow<Loadable<T>>,
) {
    scope.launch {
        payload.collect { payloadOrLoading ->
            payloadOrLoading.fold(
                ifLoading = {},
                ifReady = { payload ->
                    publish(
                        client = client,
                        payload = payload,
                    )
                }
            )
        }
    }
}

@PublishedApi
internal fun <T, P : PropertyType<T>> Property<T, P, Property.Direction.In>.subscribePrivate(
    client: MqttClient,
): Flow<T> = client
    .subscribe(
        topic = topic.raw,
    )
    .transformLatest { payload ->
        logger
            .tryOrLog(
                log = "parse mqtt payload to typed json",
            ) {
                Json.decodeFromString(
                    deserializer = type.serializer,
                    string = payload.payload.decodeToString(),
                )
            }
            .onSuccess { emit(it) }
    }

fun <T, P : PropertyType.Events<T>> Property<T, P, Property.Direction.In>.subscribe(
    client: MqttClient,
): Flow<T> = subscribePrivate(
    client = client,
)

val PropertyFallbackDelay: Duration = 3.seconds

inline fun <T, P : PropertyType.State<T>> Property<T, P, Property.Direction.In>.subscribe(
    scope: CoroutineScope,
    client: MqttClient,
    fallbackDelay: Duration = PropertyFallbackDelay,
    crossinline getFallbackValue: suspend () -> T,
): MutableStateFlow<Loadable<T>> {
    val result: MutableStateFlow<Loadable<T>> = Loading.toMutableStateFlowAsInitial()
    scope.launch {
        subscribePrivate(
            client = client,
        )
            .map(::Ready)
            .collect(result)
    }
    scope.launch {
        try {
            withTimeout(
                timeout = fallbackDelay,
            ) {
                result.first { valueOrLoading ->
                    when (valueOrLoading) {
                        Loading -> false
                        is Ready -> true
                    }
                }
            }
        } catch (_: TimeoutCancellationException) {
            val fallbackValue = getFallbackValue()
            publishPrivate(
                client = client,
                payload = fallbackValue,
                retained = true,
            )
        }
    }
    return result
}


fun <T, P : PropertyType<T>, D : Property.Direction> Property<T, P, D>.element(
    title: String = "",
): ElementWithChildren<ElementWithChildren.Type.Property<T>> = ElementWithChildren(
    topic = topic,
    title = title,
    type = ElementWithChildren.Type.Property(
        type = type,
        mode = when (direction) {
            is Property.Direction.In -> when (direction.origin) {
                InPropertyOrigin.Manual -> PropertyMode.Manual
                InPropertyOrigin.Hardware -> PropertyMode.Hardware
            }

            Property.Direction.Calculated -> PropertyMode.Calculated
        },
    ),
)