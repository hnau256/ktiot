package hnau.ktiot.coordinator

import kotlinx.coroutines.flow.StateFlow
import org.hnau.commons.kotlin.Ready
import org.hnau.commons.kotlin.coroutines.flow.state.mutable.toMutableStateFlowAsInitial

fun <T>T.asReadyStateFlow(): StateFlow<Ready<T>> =
    let(::Ready).toMutableStateFlowAsInitial()