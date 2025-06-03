package hnau.ktiot.client.model.init

import hnau.common.model.goback.GoBackHandler
import hnau.ktiot.client.model.LoggedModel
import hnau.ktiot.client.model.LoginModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface InitStateModel {

    val id: Int

    val goBackHandler: GoBackHandler

    data class Login(
        val model: LoginModel,
    ) : InitStateModel {

        override val id: Int
            get() = 0

        override val goBackHandler: GoBackHandler
            get() = model.goBackHandler
    }

    data class Logged(
        val model: LoggedModel,
    ) : InitStateModel {

        override val id: Int
            get() = 1

        override val goBackHandler: GoBackHandler
            get() = model.goBackHandler
    }

    @Serializable
    sealed interface Skeleton {

        @Serializable
        @SerialName("login")
        data class Login(
            val skeleton: LoginModel.Skeleton,
        ) : Skeleton

        @Serializable
        @SerialName("logged")
        data class Logged(
            val skeleton: LoggedModel.Skeleton,
        ) : Skeleton
    }
}