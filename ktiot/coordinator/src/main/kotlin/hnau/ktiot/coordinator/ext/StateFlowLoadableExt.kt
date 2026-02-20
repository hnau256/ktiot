package hnau.ktiot.coordinator.ext

import hnau.common.kotlin.Loadable
import hnau.common.kotlin.Loading
import hnau.common.kotlin.coroutines.flow.state.flatMapWithScope
import hnau.common.kotlin.coroutines.flow.state.mapState
import hnau.common.kotlin.coroutines.flow.state.mutable.toMutableStateFlowAsInitial
import hnau.common.kotlin.fold
import hnau.common.kotlin.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

inline fun <A, B, Z> StateFlow<Loadable<A>>.combineLoadableStateWith(
    scope: CoroutineScope,
    other: StateFlow<Loadable<B>>,
    crossinline combine: (A, B) -> Z,
): StateFlow<Loadable<Z>> = flatMapWithScope(scope) { scope, aOrLoading ->
    aOrLoading.fold(
        ifLoading = { Loading.toMutableStateFlowAsInitial() },
        ifReady = { a ->
            other.mapState(scope) { bOrLoading ->
                bOrLoading.map { b ->
                    combine(a, b)
                }
            }
        }
    )
}