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
import kotlinx.coroutines.runBlocking
import org.slf4j.simple.SimpleLogger

@OptIn(InternalComposeApi::class)
fun main() = runBlocking {
    System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE")
    val app = DesktopApp(
        scope = this,
        seed = createPinFinAppSeed(
            defaultBrightness = ThemeBrightness.Dark,
        ),
    )
    val projector = createAppProjector(
        scope = this,
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