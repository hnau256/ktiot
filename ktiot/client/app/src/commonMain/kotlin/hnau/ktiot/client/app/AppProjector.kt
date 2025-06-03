package hnau.ktiot.client.app

import hnau.common.model.app.AppModel
import hnau.common.projector.app.AppProjector
import hnau.ktiot.client.model.init.InitModel
import hnau.ktiot.client.projector.init.InitProjector
import hnau.ktiot.client.projector.init.impl
import kotlinx.coroutines.CoroutineScope

fun createAppProjector(
    scope: CoroutineScope,
    model: AppModel<InitModel, InitModel.Skeleton>,
): AppProjector<InitModel, InitModel.Skeleton, InitProjector> = AppProjector(
    scope = scope,
    model = model,
    createProjector = { scope, model, globalGoBackHandler ->
        InitProjector(
                scope = scope,
                model = model,
                dependencies = InitProjector.Dependencies.impl(),
            )
    },
    content = { rootProjector ->
        rootProjector.Content()
    }
)