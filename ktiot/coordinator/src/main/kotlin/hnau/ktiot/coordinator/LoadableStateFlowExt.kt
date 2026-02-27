package hnau.ktiot.coordinator

import org.hnau.commons.kotlin.Ready
import org.hnau.commons.kotlin.coroutines.flow.state.mutable.toMutableStateFlowAsInitial
import kotlinx.coroutines.flow.StateFlow

fun <T>T.asReadyStateFlow(): StateFlow<Ready<T>> =
    let(::Ready).toMutableStateFlowAsInitial()