@file:UseSerializers(
    MutableStateFlowSerializer::class,
)

package hnau.ktiot.client.model.utils

import hnau.common.model.goback.GoBackHandler
import hnau.common.model.goback.GoBackHandlerProvider
import hnau.common.kotlin.serialization.MutableStateFlowSerializer
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

class TemplateModel(
    scope: CoroutineScope,
    dependencies: Dependencies,
    skeleton: Skeleton,
) {

    @Pipe
    interface Dependencies {

    }

    @Serializable
    /*data*/ class Skeleton

    val goBackHandler: GoBackHandler = TODO()
}