@file:UseSerializers(
    MutableStateFlowSerializer::class,
)

package hnau.ktiot.client.model

import hnau.common.kotlin.coroutines.actionOrNullIfExecuting
import hnau.common.kotlin.coroutines.flatMapState
import hnau.common.kotlin.coroutines.flow.state.mapState
import hnau.common.kotlin.coroutines.flow.state.mapWithScope
import hnau.common.kotlin.coroutines.flow.state.scopedInState
import hnau.common.kotlin.getOrInit
import hnau.common.kotlin.serialization.MutableStateFlowSerializer
import hnau.common.kotlin.toAccessor
import hnau.common.model.goback.GoBackHandler
import hnau.common.mqtt.mqtt
import hnau.common.mqtt.utils.MqttClient
import hnau.common.mqtt.utils.MqttConfig
import hnau.common.mqtt.utils.MqttState
import hnau.ktiot.client.model.init.DoLogout
import hnau.ktiot.client.model.init.LoginInfo
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

class LoggedModel(
    scope: CoroutineScope,
    dependencies: Dependencies,
    skeleton: Skeleton,
) {

    @Pipe
    interface Dependencies {

        val doLogout: DoLogout

        fun connected(
            mqttClient: MqttClient,
        ): ConnectedModel.Dependencies
    }

    sealed interface State {

        data class Connecting(
            val logout: StateFlow<(() -> Unit)?>,
        ) : State

        data class WaitingForReconnection(
            val cause: Throwable,
            val reconnectionAt: Instant,
            val logout: StateFlow<(() -> Unit)?>,
            val reconnectNow: () -> Unit,
        ) : State

        data class Connected(
            val model: ConnectedModel,
        ) : State
    }

    @Serializable
    data class Skeleton(
        val loginInfo: LoginInfo,
        var connected: ConnectedModel.Skeleton? = null,
    )

    private val logout: StateFlow<(() -> Unit)?> = actionOrNullIfExecuting(scope) {
        dependencies.doLogout.doLogout()
    }

    val state: StateFlow<State> = mqtt(
        scope = scope,
        config = MqttConfig(
            address = skeleton.loginInfo.address,
            port = skeleton.loginInfo.port,
            clientId = skeleton.loginInfo.clientId,
            auth = skeleton.loginInfo.auth?.let { auth ->
                MqttConfig.Auth(
                    user = auth.user,
                    password = auth.password,
                )
            }
        ),
    )
        .mapWithScope(scope) { stateScope, mqttState ->
            when (mqttState) {
                is MqttState.Connected -> State.Connected(
                    model = ConnectedModel(
                        scope = stateScope,
                        dependencies = dependencies.connected(
                            mqttClient = mqttState.client,
                        ),
                        skeleton = skeleton::connected
                            .toAccessor()
                            .getOrInit { ConnectedModel.Skeleton() }
                    )
                )

                MqttState.Connecting -> State.Connecting(
                    logout = logout,
                )

                is MqttState.WaitingForReconnection -> State.WaitingForReconnection(
                    cause = mqttState.cause,
                    reconnectionAt = mqttState.reconnectionAt,
                    logout = logout,
                    reconnectNow = mqttState.reconnectNow,
                )
            }
        }

    val goBackHandler: GoBackHandler = state
        .scopedInState(scope)
        .flatMapState(scope) { (stateScope, state) ->
            when (state) {
                is State.Connected -> state.model.goBackHandler

                is State.Connecting -> state.logout.mapState(stateScope) { logout ->
                    { logout?.invoke() }
                }

                is State.WaitingForReconnection -> state.logout.mapState(stateScope) { logout ->
                    { logout?.invoke() }
                }
            }
        }
}