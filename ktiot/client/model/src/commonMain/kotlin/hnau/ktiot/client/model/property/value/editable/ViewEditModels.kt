package hnau.ktiot.client.model.property.value.editable

import arrow.core.Option
import hnau.common.app.model.goback.GoBackHandler
import hnau.ktiot.scheme.PropertyType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

sealed interface ViewModel {

    fun interface Factory<T, P : PropertyType.State<T>, D, S : Skeleton, M : ViewModel> {

        fun createViewModel(
            scope: CoroutineScope,
            dependencies: D,
            skeleton: S,
            type: P,
            value: StateFlow<T>,
        ): M
    }

    @Serializable
    sealed interface Skeleton

    val goBackHandler: GoBackHandler
}

sealed interface EditModel<out T> {

    fun interface Factory<T, P : PropertyType.State<T>, D, S : Skeleton, M : EditModel<T>> {

        fun createEditModel(
            scope: CoroutineScope,
            dependencies: D,
            skeleton: S,
            type: P,
            enabled: StateFlow<Boolean>,
        ): M
    }

    @Serializable
    sealed interface Skeleton

    val value: StateFlow<Option<T>>

    val goBackHandler: GoBackHandler
}