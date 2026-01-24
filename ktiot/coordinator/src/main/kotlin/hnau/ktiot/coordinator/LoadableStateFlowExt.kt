package hnau.ktiot.coordinator

import hnau.common.kotlin.Ready
import hnau.common.kotlin.coroutines.flow.state.mutable.toMutableStateFlowAsInitial
import kotlinx.coroutines.flow.StateFlow

fun <T>T.asReadyStateFlow(): StateFlow<Ready<T>> =
    let(::Ready).toMutableStateFlowAsInitial()