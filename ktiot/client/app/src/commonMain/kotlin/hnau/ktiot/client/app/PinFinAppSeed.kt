package hnau.ktiot.client.app

import org.hnau.commons.app.model.app.AppSeed
import org.hnau.commons.app.model.file.plus
import org.hnau.commons.app.model.preferences.impl.FileBasedPreferences
import org.hnau.commons.app.model.theme.ThemeBrightness
import org.hnau.commons.app.model.utils.Hue
import hnau.ktiot.client.model.init.InitModel
import hnau.ktiot.client.model.init.impl

fun createPinFinAppSeed(
    defaultBrightness: ThemeBrightness? = null,
): AppSeed<InitModel, InitModel.Skeleton> = AppSeed(
    fallbackHue = Hue(320),
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
)