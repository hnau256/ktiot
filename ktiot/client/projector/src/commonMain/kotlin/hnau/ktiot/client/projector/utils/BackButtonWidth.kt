package hnau.ktiot.client.projector.utils

import androidx.compose.ui.unit.Dp
import hnau.common.app.projector.uikit.backbutton.BackButtonProjector

interface BackButtonWidth {

    val width: Dp

    companion object {

        fun create(
            backButtonProjector: BackButtonProjector,
        ): BackButtonWidth = object : BackButtonWidth {

            override val width: Dp
                get() = backButtonProjector.width

        }
    }
}