package hnau.ktiot.client.projector.init

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import hnau.common.kotlin.Loadable
import hnau.common.kotlin.coroutines.mapWithScope
import hnau.common.kotlin.map
import hnau.common.projector.uikit.state.LoadableContent
import hnau.common.projector.uikit.state.TransitionSpec
import hnau.ktiot.client.model.init.InitModel
import hnau.ktiot.client.model.init.InitStateModel
import hnau.ktiot.client.projector.LoggedProjector
import hnau.ktiot.client.projector.LoginProjector
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

class InitProjector(
    scope: CoroutineScope,
    model: InitModel,
    dependencies: Dependencies,
) {

    @Pipe
    interface Dependencies {

        fun login(): LoginProjector.Dependencies

        fun logged(): LoggedProjector.Dependencies

        companion object
    }

    private val state: StateFlow<Loadable<InitStateProjector>> = model
        .state
        .mapWithScope(scope) { stateScope, stateOrLoading ->
            stateOrLoading.map { state ->
                when (state) {
                    is InitStateModel.Login -> InitStateProjector.Login(
                        projector = LoginProjector(
                            scope = stateScope,
                            model = state.model,
                            dependencies = dependencies.login(),
                        )
                    )

                    is InitStateModel.Logged -> InitStateProjector.Logged(
                        projector = LoggedProjector(
                            scope = stateScope,
                            model = state.model,
                            dependencies = dependencies.logged(),
                        )
                    )
                }
            }
        }

    @Composable
    fun Content() {
        state
            .collectAsState()
            .value
            .LoadableContent(
                modifier = Modifier.fillMaxSize(),
                transitionSpec = TransitionSpec.crossfade(),
            ) { stateProjector ->
                stateProjector.Content()
            }
    }
}