package hnau.ktiot.client.model.property.value

import hnau.common.kotlin.coroutines.flow.state.flatMapState
import hnau.common.kotlin.coroutines.flow.state.mapState
import hnau.common.kotlin.coroutines.flow.state.mutable.toMutableStateFlowAsInitial
import hnau.common.kotlin.foldNullable
import hnau.common.app.model.goback.GoBackHandler
import hnau.common.app.model.goback.NeverGoBackHandler
import hnau.ktiot.scheme.PropertyType
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class FractionModel(
    private val scope: CoroutineScope,
    dependencies: Dependencies,
    skeleton: Skeleton,
    value: StateFlow<Float>,
    private val publish: StateFlow<((Float) -> Unit)?>,
    val type: PropertyType.State.Fraction,
    val mutable: Boolean,
) : ValueModel {

    companion object {

        val factory: ValueModel.Factory<Float, PropertyType.State.Fraction, Dependencies, Skeleton, FractionModel> =
            ValueModel.Factory(::FractionModel)
    }

    @Pipe
    interface Dependencies

    @Serializable
    /*data*/ class Skeleton : ValueModel.Skeleton

    private val overwriteValue: MutableStateFlow<Float?> = MutableStateFlow(null)

    val value: StateFlow<Float> = overwriteValue.flatMapState(scope) { overwriteOrNull ->
        overwriteOrNull.foldNullable(
            ifNull = { value },
            ifNotNull = { it.toMutableStateFlowAsInitial() },
        )
    }

    fun update(
        newValue: Float,
    ) {
        overwriteValue.value = newValue
    }

    val isPublishing: StateFlow<Boolean> =
        publish.mapState(scope) { it == null }

    fun publish() {
        val publish = publish.value ?: return
        val valueToPublish = overwriteValue.value ?: return
        publish(valueToPublish)
        scope.launch {//TODO
            delay(10)
            overwriteValue.value = null
        }
    }

    override val goBackHandler: GoBackHandler
        get() = NeverGoBackHandler
}