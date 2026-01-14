package hnau.ktiot.scheme

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PropertyMode {

    @SerialName("hardware")
    Hardware,

    @SerialName("manual")
    Manual,

    @SerialName("calculated")
    Calculated,
}