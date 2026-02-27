package hnau.ktiot.coordinator.ext

import org.hnau.commons.kotlin.Loadable
import org.hnau.commons.kotlin.Loading
import org.hnau.commons.kotlin.coroutines.flow.state.flatMapWithScope
import org.hnau.commons.kotlin.coroutines.flow.state.mapState
import org.hnau.commons.kotlin.coroutines.flow.state.mutable.toMutableStateFlowAsInitial
import org.hnau.commons.kotlin.fold
import org.hnau.commons.kotlin.map
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