package hnau.ktiot.client.projector

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import hnau.common.app.model.goback.GlobalGoBackHandler
import hnau.common.app.model.goback.GoBackHandler
import hnau.common.app.projector.utils.NavigationIcon
import hnau.ktiot.client.model.ConnectedModel
import hnau.ktiot.client.projector.screen.ScreenProjector
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope

@Immutable
class ConnectedProjector(
    scope: CoroutineScope,
    model: ConnectedModel,
    dependencies: Dependencies,
) {

    @Immutable
    @Pipe
    interface Dependencies {

        val globalGoBackHandler: GlobalGoBackHandler

        fun screen(): ScreenProjector.Dependencies
    }

    private val globalGoBackHandler: GoBackHandler = dependencies
        .globalGoBackHandler
        .resolve(scope)

    private val rootScreen = ScreenProjector(
        scope = scope,
        model = model.rootScreen,
        dependencies = dependencies.screen(),
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Content() {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    navigationIcon = { globalGoBackHandler.NavigationIcon() },
                    title = {},
                )
            }
        ) { contentPadding ->
            rootScreen.Content(
                contentPadding = contentPadding,
            )
        }
    }
}