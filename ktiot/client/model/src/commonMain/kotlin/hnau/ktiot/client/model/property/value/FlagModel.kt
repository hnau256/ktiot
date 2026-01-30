package hnau.ktiot.client.model.property.value

import hnau.common.app.model.goback.GoBackHandler
import hnau.common.app.model.goback.NeverGoBackHandler
import hnau.ktiot.scheme.PropertyType
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

class FlagModel(
    private val scope: CoroutineScope,
    dependencies: Dependencies,
    skeleton: Skeleton,
    val value: StateFlow<Boolean>,
    val publish: StateFlow<((Boolean) -> Unit)?>,
    type: PropertyType.State.Flag,
    val mutable: Boolean,
) : ValueModel {

    companion object {

        val factory: ValueModel.Factory<Boolean, PropertyType.State.Flag, Dependencies, Skeleton, FlagModel> =
            ValueModel.Factory(::FlagModel)
    }

    @Pipe
    interface Dependencies

    @Serializable
    /*data*/ class Skeleton : ValueModel.Skeleton

    override val goBackHandler: GoBackHandler
        get() = NeverGoBackHandler
}