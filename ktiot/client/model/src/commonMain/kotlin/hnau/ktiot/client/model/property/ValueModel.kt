package hnau.ktiot.client.model.property

import hnau.common.model.goback.GoBackHandler

sealed interface ValueModel {

    sealed interface Skeleton

    val goBackHandler: GoBackHandler
}