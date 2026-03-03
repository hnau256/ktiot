package hnau.common.mqtt.types

data class BrokerConfig(
    val connection: Connection,
    val messagesBufferSize: Int = 256,
) {

    data class Connection(
        val host: String,
        val port: Int = defaultPort,
        val clientId: String,
        val auth: Auth? = null,
        val protocol: Protocol = Protocol.TCP,
    ) {

        data class Auth(
            val user: String,
            val password: String,
        )

        enum class Protocol { TCP, SSL }

        companion object {

            val defaultPort: Int = 1883
        }
    }
}