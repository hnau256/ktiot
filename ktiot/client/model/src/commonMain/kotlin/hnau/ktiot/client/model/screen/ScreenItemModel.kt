package hnau.ktiot.client.model.screen

import hnau.common.model.goback.GoBackHandler
import hnau.common.model.goback.NeverGoBackHandler
import hnau.ktiot.client.model.property.PropertyModel

sealed interface ScreenItemModel {

    val key: Int

    val goBackHandler: GoBackHandler

    data class Property(
        val model: PropertyModel,
    ) : ScreenItemModel {

        override val key: Int
            get() = 0

        override val goBackHandler: GoBackHandler
            get() = model.goBackHandler
    }

    data object ChildButton: ScreenItemModel {

        override val key: Int
            get() = 1

        override val goBackHandler: GoBackHandler
            get() = NeverGoBackHandler

    }

    sealed interface Skeleton {

        data class Property(
            val skeleton: PropertyModel.Skeleton,
        ): Skeleton
    }
}