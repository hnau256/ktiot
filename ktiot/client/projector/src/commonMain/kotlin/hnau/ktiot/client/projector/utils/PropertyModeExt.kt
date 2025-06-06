package hnau.ktiot.client.projector.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.ui.graphics.vector.ImageVector
import hnau.ktiot.scheme.PropertyMode

val PropertyMode.icon: ImageVector
    get() = when (this) {
        PropertyMode.Hardware -> Icons.Filled.Thermostat
        PropertyMode.Manual -> Icons.Filled.TouchApp
        PropertyMode.Calculated -> Icons.Filled.Calculate
    }