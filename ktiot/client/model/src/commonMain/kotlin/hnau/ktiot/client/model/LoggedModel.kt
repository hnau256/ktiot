@file:UseSerializers(
    MutableStateFlowSerializer::class,
)

package hnau.ktiot.client.model

import org.hnau.commons.kotlin.coroutines.actionOrNullIfExecuting
import org.hnau.commons.kotlin.coroutines.flow.state.flatMapState
import org.hnau.commons.kotlin.coroutines.flow.state.mapState
import org.hnau.commons.kotlin.coroutines.flow.state.mapWithScope
import org.hnau.commons.kotlin.coroutines.flow.state.scopedInState
import org.hnau.commons.kotlin.getOrInit
import org.hnau.commons.kotlin.serialization.MutableStateFlowSerializer
import org.hnau.commons.kotlin.toAccessor
import org.hnau.commons.app.model.goback.GoBackHandler
import hnau.common.mqtt.mqtt
import hnau.common.mqtt.types.BrokerConfig
import hnau.common.mqtt.types.MqttSession
import hnau.common.mqtt.types.MqttConfig
import hnau.common.mqtt.types.MqttState
import hnau.ktiot.client.model.init.DoLogout
import hnau.ktiot.client.model.init.LoginInfo
import org.hnau.commons.gen.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.hnau.commons.kotlin.castOrThrow

class LoggedModel(
    scope: CoroutineScope,
    dependencies: Dependencies,
    skeleton: Skeleton,
) {

    @Pipe
    interface Dependencies {

        val doLogout: DoLogout

        fun connected(
            mqttClient: MqttSession,
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
            broker = BrokerConfig(
                connection = BrokerConfig.Connection(
                    host = skeleton.loginInfo.address,
                    port = skeleton.loginInfo.port,
                    clientId = skeleton.loginInfo.clientId,
                    auth = skeleton.loginInfo.auth?.let { auth ->
                        BrokerConfig.Connection.Auth(
                            user = auth.user,
                            password = auth.password,
                        )
                    }
                )
            ),
        ),
    )
        .mapWithScope(scope) { stateScope, mqttState ->
            when (mqttState) {
                is MqttState.Connected -> State.Connected(
                    model = ConnectedModel(
                        scope = stateScope,
                        dependencies = dependencies.connected(
                            mqttClient = mqttState.session,
                        ),
                        skeleton = skeleton::connected
                            .toAccessor()
                            .getOrInit { ConnectedModel.Skeleton() }
                    )
                )

                MqttState.Connecting -> State.Connecting(
                    logout = logout,
                )

                is MqttState.WaitingForReconnect -> State.WaitingForReconnection(
                    cause = mqttState.disconnectedError.cause, //TODO use full MqttResult.Error
                    reconnectionAt = mqttState.nextAttemptAt,
                    logout = logout,
                    reconnectNow = mqttState.connectNow,
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