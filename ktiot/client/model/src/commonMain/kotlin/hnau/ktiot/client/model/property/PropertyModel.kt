package hnau.ktiot.client.model.property

import hnau.common.kotlin.Loadable
import hnau.common.kotlin.coroutines.flatMapState
import hnau.common.kotlin.fold
import hnau.common.model.goback.GoBackHandler
import hnau.common.model.goback.NeverGoBackHandler
import hnau.common.mqtt.utils.MqttClient
import hnau.ktiot.client.model.property.value.EditableModel
import hnau.ktiot.client.model.property.value.FlagModel
import hnau.ktiot.client.model.property.value.FractionModel
import hnau.ktiot.client.model.property.value.ValueModel
import hnau.ktiot.client.model.property.value.createValueModel
import hnau.ktiot.client.model.property.value.editable.EditModel
import hnau.ktiot.client.model.property.value.editable.TextEditModel
import hnau.ktiot.client.model.property.value.editable.TextViewModel
import hnau.ktiot.client.model.property.value.editable.ViewModel
import hnau.ktiot.scheme.Element
import hnau.ktiot.scheme.PropertyMode
import hnau.ktiot.scheme.PropertyType
import hnau.ktiot.scheme.topic.ChildTopic
import hnau.pipe.annotations.Pipe
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

private val logger = KotlinLogging.logger { }

class PropertyModel(
    private val scope: CoroutineScope,
    private val dependencies: Dependencies,
    private val skeleton: Skeleton,
    val topic: ChildTopic,
    private val property: Element.Property<*>,
) {

    @Pipe
    interface Dependencies {

        val mqttClient: MqttClient

        fun flag(): FlagModel.Dependencies

        fun fraction(): FractionModel.Dependencies

        fun editable(): EditableModel.Dependencies
    }

    @Serializable
    data class Skeleton(
        var value: ValueModel.Skeleton? = null,
    )

    val mode: PropertyMode
        get() = property.mode

    val value: StateFlow<Loadable<Result<ValueModel>>> = when (val type = property.type) {
        is PropertyType.Events -> TODO()
        is PropertyType.State -> when (type) {
            is PropertyType.State.Fraction -> createValueModel(
                valueModelFactory = FractionModel.factory,
                createInitialSkeleton = { FractionModel.Skeleton() },
                extractDependencies = Dependencies::fraction,
                type = type,
            )

            is PropertyType.State.Enum -> TODO()

            is PropertyType.State.Flag -> createValueModel(
                valueModelFactory = FlagModel.factory,
                createInitialSkeleton = { FlagModel.Skeleton() },
                extractDependencies = Dependencies::flag,
                type = type,
            )

            is PropertyType.State.Number -> TODO()

            is PropertyType.State.Text -> createEditableModel(
                createViewModelSkeleton = { TextViewModel.Skeleton() },
                extractViewDependencies = { textView() },
                viewFactory = TextViewModel.factory,
                createEditModelSkeleton = { initial -> TextEditModel.Skeleton(initial) },
                extractEditDependencies = { textEdit() },
                editFactory = TextEditModel.factory,
                type = type,
            )
        }
    }

    private inline fun <reified T, P : PropertyType.State<T>, D, reified S : ValueModel.Skeleton, M : ValueModel> createValueModel(
        valueModelFactory: ValueModel.Factory<T, P, D, S, M>,
        crossinline createInitialSkeleton: () -> S,
        crossinline extractDependencies: Dependencies.() -> D,
        type: P,
    ): StateFlow<Loadable<Result<M>>> = createValueModel(
        scope = scope,
        dependencies = dependencies,
        skeleton = skeleton,
        topic = topic,
        createInitialSkeleton = createInitialSkeleton,
        extractDependencies = extractDependencies,
        valueModelFactory = valueModelFactory,
        type = type,
        mode = property.mode,
    )

    private inline fun <
            reified T, P : PropertyType.State<T>,
            V : ViewModel, VS : ViewModel.Skeleton, VD,
            E : EditModel<T>, ES : EditModel.Skeleton, ED,
            > createEditableModel(
        noinline createViewModelSkeleton: () -> VS,
        noinline extractViewDependencies: EditableModel.Dependencies.() -> VD,
        viewFactory: ViewModel.Factory<T, P, VD, VS, V>,
        noinline createEditModelSkeleton: (initialValue: T) -> ES,
        noinline extractEditDependencies: EditableModel.Dependencies.() -> ED,
        editFactory: EditModel.Factory<T, P, ED, ES, E>,
        type: P,
    ): StateFlow<Loadable<Result<EditableModel<T, P, V, VS, VD, E, ES, ED>>>> = hnau.ktiot.client.model.property.value.createEditableModel(
        scope = scope,
        dependencies = dependencies,
        skeleton = skeleton,
        topic = topic,
        type = type,
        mode = property.mode,
        createViewModelSkeleton = createViewModelSkeleton,
        extractViewDependencies = extractViewDependencies,
        viewFactory = viewFactory,
        createEditModelSkeleton = createEditModelSkeleton,
        extractEditDependencies = extractEditDependencies,
        editFactory = editFactory,
    )

    val goBackHandler: GoBackHandler = value.flatMapState(scope) { valueOrErrorLoading ->
        valueOrErrorLoading.fold(
            ifLoading = { NeverGoBackHandler },
            ifReady = { valueOrError ->
                valueOrError.fold(
                    onFailure = { NeverGoBackHandler },
                    onSuccess = ValueModel::goBackHandler,
                )
            }
        )
    }
}