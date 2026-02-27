package org.hnau.commons.mqtt.utils

data class MqttConfig(
    val address: String,
    val clientId: String,
    val auth: Auth? = null,
    val port: Int = DefaultPort,
    val protocol: Protocol = Protocol.default,
) {

    enum class Protocol(
        val uriName: String,
    ) {
        TCP(
            uriName = "tcp",
        ),
        ;

        companion object {

            val default: Protocol
                get() = TCP
        }
    }

    data class Auth(
        val user: String,
        val password: String,
    )

    companion object {

        const val DefaultPort: Int = 1883
    }
}