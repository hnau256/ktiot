package hnau.ktiot.client.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import hnau.common.model.ThemeBrightness
import hnau.common.model.app.DesktopApp
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.simple.SimpleLogger

private val logger = KotlinLogging.logger { }

@OptIn(InternalComposeApi::class)
fun main()  {
    System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE")

    val appScope = CoroutineScope( SupervisorJob())
    val app = DesktopApp(
        scope = appScope,
        seed = createPinFinAppSeed(
            defaultBrightness = ThemeBrightness.Dark,
        ),
    )
    val projector = createAppProjector(
        scope = appScope,
        model = app,
    )
    application {
        val scale = 2f
        Window(
            onCloseRequest = { exitApplication() },
            title = "KtIoT",
            state = rememberWindowState(
                width = 480.dp * scale,
                height = 640.dp * scale,
            ),
            //icon = rememberVectorPainter(pinfinIcon.s256),
        ) {
            CompositionLocalProvider(
                LocalDensity provides Density(scale),
            ) {
                projector.Content()
            }
        }
    }
}