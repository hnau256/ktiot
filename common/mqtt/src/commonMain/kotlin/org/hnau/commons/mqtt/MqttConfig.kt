package org.hnau.commons.mqtt

data class MqttConfig(
    val host: String,
    val port: Int = 1883,
    val clientId: String,
    val auth: Auth? = null,
    val protocol: Protocol = Protocol.TCP,
) {
    data class Auth(
        val username: String,
        val password: String,
    )

    enum class Protocol { TCP, SSL }
}
