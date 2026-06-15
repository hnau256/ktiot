package org.hnau.ktiot.mqtt.types

import org.hnau.commons.gen.fold.annotations.Fold

data class BrokerConfig(
    val connection: Connection,
    val messagesBufferSize: Int = 256,
) {

    data class Connection(
        val host: ServerHost,
        val port: Int = defaultPort,
        val clientId: String,
        val auth: Auth? = null,
        val protocol: Protocol = Protocol.TCP,
    ) {

        data class Auth(
            val user: String,
            val password: String,
        )

        @Fold
        enum class Protocol {
            TCP, SSL;

            companion object {

                val default: Protocol
                    get() = TCP
            }
        }

        companion object {

            val defaultPort: Int = 1883
        }
    }
}