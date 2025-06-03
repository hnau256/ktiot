package hnau.ktiot.client.app

import hnau.common.model.ThemeBrightness
import hnau.common.model.app.AppSeed
import hnau.common.model.color.material.MaterialHue
import hnau.common.model.file.plus
import hnau.common.model.preferences.impl.FileBasedPreferences
import hnau.ktiot.client.model.init.InitModel
import hnau.ktiot.client.model.init.impl

fun createPinFinAppSeed(
    defaultBrightness: ThemeBrightness? = null,
): AppSeed<InitModel, InitModel.Skeleton> = AppSeed(
    fallbackHue = MaterialHue.LightGreen,
    defaultBrightness = defaultBrightness,
    skeletonSerializer = InitModel.Skeleton.serializer(),
    createDefaultSkeleton = { InitModel.Skeleton() },
    createModel = { scope, appContext, skeleton ->
        InitModel(
                scope = scope,
                dependencies = InitModel.Dependencies.impl(
                    preferencesFactory = FileBasedPreferences.Factory(
                        preferencesFile = appContext.filesDir + "preferences.txt"
                    )
                ),
                skeleton = skeleton,
            )
    },
    extractGoBackHandler = InitModel::goBackHandler,
)