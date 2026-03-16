package org.hnau.ktiot.coordinator

interface AlertRegistry {

    suspend fun registerAlert(
        message: String,
    ): Nothing
}