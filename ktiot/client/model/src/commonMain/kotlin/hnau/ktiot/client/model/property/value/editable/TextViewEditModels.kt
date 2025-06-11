@file:UseSerializers(
    MutableStateFlowSerializer::class,
)

package hnau.ktiot.client.model.property.value.editable

import arrow.core.Option
import arrow.core.some
import hnau.common.kotlin.coroutines.mapState
import hnau.common.kotlin.coroutines.toMutableStateFlowAsInitial
import hnau.common.kotlin.serialization.MutableStateFlowSerializer
import hnau.common.model.EditingString
import hnau.common.model.goback.GoBackHandler
import hnau.common.model.goback.NeverGoBackHandler
import hnau.common.model.toEditingString
import hnau.ktiot.scheme.PropertyType
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

class TextViewModel(
    scope: CoroutineScope,
    dependencies: Dependencies,
    skeleton: Skeleton,
    type: PropertyType.State.Text,
    val value: StateFlow<String>,
) : ViewModel {

    companion object {

        val factory: ViewModel.Factory<String, PropertyType.State.Text, Dependencies, Skeleton, TextViewModel> =
            ViewModel.Factory(::TextViewModel)
    }

    @Pipe
    interface Dependencies

    @Serializable
    @SerialName("text")
    /*data*/ class Skeleton : ViewModel.Skeleton

    override val goBackHandler: GoBackHandler
        get() = NeverGoBackHandler
}

class TextEditModel(
    scope: CoroutineScope,
    dependencies: Dependencies,
    private val skeleton: Skeleton,
    type: PropertyType.State.Text,
    val enabled: StateFlow<Boolean>,
) : EditModel<String> {

    companion object {

        val factory: EditModel.Factory<String, PropertyType.State.Text, Dependencies, Skeleton, TextEditModel> =
            EditModel.Factory(::TextEditModel)
    }

    @Pipe
    interface Dependencies

    @Serializable
    @SerialName("text")
    data class Skeleton(
        val input: MutableStateFlow<EditingString>,
    ) : EditModel.Skeleton {

        constructor(
            initial: String?,
        ) : this(
            input = initial
                .orEmpty()
                .toEditingString()
                .toMutableStateFlowAsInitial(),
        )
    }

    val input: MutableStateFlow<EditingString>
        get() = skeleton.input

    override val value: StateFlow<Option<String>> = skeleton
        .input
        .mapState(scope) { input -> input.text.some() }

    override val goBackHandler: GoBackHandler
        get() = NeverGoBackHandler

}