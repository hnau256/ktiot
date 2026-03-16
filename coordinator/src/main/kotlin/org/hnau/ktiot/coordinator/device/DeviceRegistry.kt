package org.hnau.ktiot.coordinator.device

interface DeviceRegistry {

    suspend fun registerDevice(
        device: Device,
    ): Nothing
}