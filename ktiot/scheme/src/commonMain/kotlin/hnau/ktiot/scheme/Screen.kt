package hnau.ktiot.scheme

import kotlinx.serialization.Serializable

@Serializable
data class Screen(
    val elements: List<Element>,
)