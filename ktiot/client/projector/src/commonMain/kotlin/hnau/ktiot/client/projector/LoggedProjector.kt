package hnau.ktiot.client.projector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastJoinToString
import hnau.common.kotlin.coroutines.flow.state.mapWithScope
import hnau.common.app.projector.uikit.ErrorPanel
import hnau.common.app.projector.uikit.state.StateContent
import hnau.common.app.projector.uikit.state.TransitionSpec
import hnau.common.app.projector.uikit.utils.Dimens
import hnau.ktiot.client.model.LoggedModel
import hnau.ktiot.client.projector.utils.Button
import hnau.ktiot.client.projector.utils.format
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Clock
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import androidx.compose.material3.Button as MaterialButton

@Immutable
class LoggedProjector(
    scope: CoroutineScope,
    model: LoggedModel,
    dependencies: Dependencies,
) {

    @Immutable
    @Pipe
    interface Dependencies {

        fun connected(): ConnectedProjector.Dependencies
    }

    @Immutable
    sealed interface State {

        @Immutable
        data class Connecting(
            val logout: StateFlow<(() -> Unit)?>,
        ) : State

        @Immutable
        data class WaitingForReconnection(
            val errorMessage: String?,
            val beforeReconnection: StateFlow<Duration>,
            val logout: StateFlow<(() -> Unit)?>,
            val reconnectNow: () -> Unit,
        ) : State

        @Immutable
        data class Connected(
            val projector: ConnectedProjector,
        ) : State
    }

    private val state: StateFlow<State> = model
        .state
        .mapWithScope(scope) { sateScope, state ->
            when (state) {
                is LoggedModel.State.Connected -> State.Connected(
                    projector = ConnectedProjector(
                        scope = sateScope,
                        model = state.model,
                        dependencies = dependencies.connected(),
                    )
                )

                is LoggedModel.State.Connecting -> State.Connecting(
                    logout = state.logout,
                )

                is LoggedModel.State.WaitingForReconnection -> State.WaitingForReconnection(
                    errorMessage = state.cause.message,
                    logout = state.logout,
                    reconnectNow = state.reconnectNow,
                    beforeReconnection = run {
                        val calc = { state.reconnectionAt - Clock.System.now() }
                        ticker(
                            delayMillis = 1.seconds.inWholeMilliseconds,
                        )
                            .consumeAsFlow()
                            .map { calc() }
                            .stateIn(
                                scope = scope,
                                started = SharingStarted.Eagerly,
                                initialValue = calc()
                            )
                    }
                )
            }
        }

    @Composable
    fun Content() {
        state
            .collectAsState()
            .value
            .StateContent(
                modifier = Modifier.fillMaxSize(),
                label = "MqttState",
                transitionSpec = TransitionSpec.crossfade(),
                contentKey = { state ->
                    when (state) {
                        is State.Connecting -> 0
                        is State.WaitingForReconnection -> 1
                        is State.Connected -> 2
                    }
                },
            ) { state ->
                when (state) {
                    is State.Connected ->
                        state.projector.Content()

                    is State.Connecting ->
                        Connecting(state)

                    is State.WaitingForReconnection ->
                        WaitingForReconnection(state)
                }
            }
    }

    @Composable
    private fun LogoutButton(
        logout: StateFlow<(() -> Unit)?>,
    ) {
        logout.Button { Text(stringResource(Res.string.logout)) }
    }

    @Composable
    private fun Connecting(
        state: State.Connecting,
    ) {
        ErrorPanel(
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.smallSeparation)
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(Res.string.connecting))
                }
            },
            button = {
                LogoutButton(
                    logout = state.logout,
                )
            }
        )
    }

    @Composable
    private fun WaitingForReconnection(
        state: State.WaitingForReconnection,
    ) {
        ErrorPanel(
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.smallSeparation)
                ) {
                    Text(
                        listOfNotNull(
                            stringResource(Res.string.connection_error),
                            state.errorMessage
                        ).fastJoinToString(
                            separator = ": ",
                        )
                    )
                    Text(
                        stringResource(Res.string.before_reconnection) +
                                ": " + state.beforeReconnection.collectAsState().value.format()
                    )
                }
            },
            button = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.smallSeparation)
                ) {
                    LogoutButton(
                        logout = state.logout,
                    )
                    MaterialButton(
                        onClick = state.reconnectNow,
                    ) {
                        Text(stringResource(Res.string.reconnect_now))
                    }
                }
            }
        )
    }
}