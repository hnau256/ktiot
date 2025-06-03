@file:UseSerializers(
    MutableStateFlowSerializer::class,
)

package hnau.ktiot.client.model

import hnau.common.model.goback.GoBackHandler
import hnau.common.model.goback.GoBackHandlerProvider
import hnau.common.kotlin.serialization.MutableStateFlowSerializer
import hnau.common.model.goback.NeverGoBackHandler
import hnau.common.model.preferences.Preferences
import hnau.ktiot.client.model.init.LoginInfo
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

class LoggedModel(
    scope: CoroutineScope,
    dependencies: Dependencies,
    skeleton: Skeleton,
) {

    @Pipe
    interface Dependencies {

        val preferences: Preferences
    }

    @Serializable
    /*data*/ class Skeleton {


        constructor(
            loginInfo: LoginInfo,
        )
    }

    val goBackHandler: GoBackHandler = NeverGoBackHandler //TODO
}