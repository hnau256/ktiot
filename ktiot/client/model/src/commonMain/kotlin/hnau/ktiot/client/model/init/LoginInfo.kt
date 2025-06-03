package hnau.ktiot.client.model.init

import kotlinx.serialization.Serializable

@Serializable
data class LoginInfo(
    val address: String? = null,
    val clientId: String? = null,
    val auth: Auth? = null,
    val port: Int? = null,
) {

    @Serializable
    data class Auth(
        val user: String,
        val password: String,
    )
}