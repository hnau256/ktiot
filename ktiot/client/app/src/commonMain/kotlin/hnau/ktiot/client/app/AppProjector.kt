package hnau.ktiot.client.app

import hnau.common.app.model.app.AppModel
import hnau.common.app.projector.app.AppProjector
import hnau.ktiot.client.model.init.InitModel
import hnau.ktiot.client.projector.init.InitProjector
import hnau.ktiot.client.projector.init.impl
import hnau.ktiot.client.projector.utils.Localization
import kotlinx.coroutines.CoroutineScope

fun createAppProjector(
    scope: CoroutineScope,
    model: AppModel<InitModel, InitModel.Skeleton>,
): AppProjector<InitModel, InitModel.Skeleton, InitProjector> = AppProjector(
    scope = scope,
    model = model,
    createProjector = { scope, model ->
        InitProjector(
                scope = scope,
                model = model,
                dependencies = InitProjector.Dependencies.impl(
                    localization = Localization(),
                ),
            )
    },
    content = { rootProjector ->
        rootProjector.Content()
    }
)