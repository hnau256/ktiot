@file:UseSerializers(
    MutableStateFlowSerializer::class,
)

package hnau.ktiot.client.model.utils

import hnau.common.kotlin.serialization.MutableStateFlowSerializer
import hnau.common.model.goback.GoBackHandler
import hnau.pipe.annotations.Pipe
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

private val logger = KotlinLogging.logger {  }

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