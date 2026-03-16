package hnau.ktiot.coordinator

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class AlertRegistryImpl : AlertRegistry {

    private val alerts: MutableStateFlow<Set<String>> =
        MutableStateFlow(emptySet())

    override suspend fun registerAlert(
        message: String,
    ): Nothing {
        try {
            alerts.update { it + message }
            awaitCancellation()
        } finally {
            alerts.update { it - message }
        }
    }
}