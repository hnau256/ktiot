package hnau.common.mqtt.platform

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient
import org.eclipse.paho.client.mqttv3.IMqttToken

internal suspend fun IMqttAsyncClient.doAsync(
    block: IMqttAsyncClient.() -> IMqttToken,
): Result<Unit>  {
    val token = try {
        block()
    } catch (ex: CancellationException) {
        throw ex
    } catch (th: Throwable) {
        return Result.failure(th)
    }
    return token.await()
}

internal fun IMqttAsyncClient.disconnectFastAndSafe(
    logger: Logger,
) {
    synchronized(this) {
        if (isConnected) {
            try {
                disconnect()
            } catch (th: Throwable) {
                logger.w(th) { "Error while disconnecting from broker" }
            }
        }
    }
}