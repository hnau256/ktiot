@file:UseSerializers(
    MutableStateFlowSerializer::class,
    EditingString.Serializer::class,
)
@file:OptIn(ExperimentalUuidApi::class)

package hnau.ktiot.client.model

import arrow.core.Option
import arrow.core.some
import arrow.core.toOption
import hnau.common.kotlin.coroutines.actionOrNullIfExecuting
import hnau.common.kotlin.coroutines.flatMapState
import hnau.common.kotlin.coroutines.mapState
import hnau.common.kotlin.coroutines.mapWithScope
import hnau.common.kotlin.coroutines.scopedInState
import hnau.common.kotlin.coroutines.toMutableStateFlowAsInitial
import hnau.common.kotlin.foldNullable
import hnau.common.kotlin.ifNull
import hnau.common.kotlin.serialization.MutableStateFlowSerializer
import hnau.common.model.EditingString
import hnau.common.model.goback.GoBackHandler
import hnau.common.model.goback.NeverGoBackHandler
import hnau.common.model.toEditingString
import hnau.common.mqtt.utils.MqttConfig
import hnau.ktiot.client.model.init.DoLogin
import hnau.ktiot.client.model.init.LoginInfo
import hnau.pipe.annotations.Pipe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class LoginModel(
    scope: CoroutineScope,
    dependencies: Dependencies,
    private val skeleton: Skeleton,
) {

    @Pipe
    interface Dependencies {

        val doLogin: DoLogin
    }

    @Serializable
    data class Skeleton(
        val cachedLoginInfo: LoginInfo?,
        val address: MutableStateFlow<EditingString>,
        val port: MutableStateFlow<EditingString>,
        val clientId: MutableStateFlow<EditingString>,
        val credentials: MutableStateFlow<Auth?>,
    ) {

        @Serializable
        data class Auth(
            val user: MutableStateFlow<EditingString>,
            val password: MutableStateFlow<EditingString>,
        ) {

            companion object {

                fun empty() = Auth(
                    user = "".toEditingString().toMutableStateFlowAsInitial(),
                    password = "".toEditingString().toMutableStateFlowAsInitial(),
                )
            }
        }

        constructor(
            cachedLoginInfo: LoginInfo?,
        ) : this(
            cachedLoginInfo = cachedLoginInfo,
            address = cachedLoginInfo?.address.orEmpty().toEditingString()
                .toMutableStateFlowAsInitial(),
            port = cachedLoginInfo?.port?.toString().orEmpty().toEditingString()
                .toMutableStateFlowAsInitial(),
            clientId = cachedLoginInfo?.clientId.orEmpty().toEditingString()
                .toMutableStateFlowAsInitial(),
            credentials = cachedLoginInfo
                ?.auth
                ?.let { auth ->
                    Auth(
                        user = auth.user.toEditingString().toMutableStateFlowAsInitial(),
                        password = auth.password.toEditingString().toMutableStateFlowAsInitial(),
                    )
                }
                .toMutableStateFlowAsInitial()
        )
    }

    interface Input {

        val editingString: MutableStateFlow<EditingString>

        val placeholder: String

        val correct: StateFlow<Boolean>
    }

    private class InputImpl<T>(
        scope: CoroutineScope,
        override val editingString: MutableStateFlow<EditingString>,
        override val placeholder: String,
        private val tryParse: (String) -> Option<T>,
    ) : Input {

        val value: StateFlow<Option<T>> = editingString.mapState(
            scope = scope,
        ) { input ->
            input
                .text
                .takeIf(String::isNotEmpty)
                .ifNull { placeholder }
                .let(tryParse)
        }

        override val correct: StateFlow<Boolean> =
            value.mapState(scope) { it.isSome() }
    }

    private val _address: InputImpl<String> = InputImpl(
        scope = scope,
        editingString = skeleton.address,
        placeholder = skeleton
            .cachedLoginInfo
            ?.address
            ?: "127.0.0.1",
        tryParse = { input ->
            input
                .trim()
                .takeIf(String::isNotEmpty)
                .toOption()
        }
    )

    val address: Input
        get() = _address

    private val _port: InputImpl<Int> = InputImpl(
        scope = scope,
        editingString = skeleton.port,
        placeholder = (skeleton.cachedLoginInfo?.port ?: MqttConfig.DefaultPort).toString(),
        tryParse = { input ->
            input
                .toIntOrNull()
                ?.takeIf { it > 0 }
                .toOption()
        }
    )

    val port: Input
        get() = _port


    private val _clientId: InputImpl<String> = InputImpl(
        scope = scope,
        editingString = skeleton.clientId,
        placeholder = skeleton
            .cachedLoginInfo
            ?.clientId
            ?: Uuid.random().toString(),
        tryParse = { input ->
            input
                .trim()
                .takeIf(String::isNotEmpty)
                .toOption()
        }
    )

    val clientId: Input
        get() = _clientId

    private data class InternalCredentials(
        val user: InputImpl<String>,
        val password: InputImpl<String>,
    )

    private val _credentials: StateFlow<InternalCredentials?> = skeleton
        .credentials
        .mapWithScope(scope) { authScope, credentialsOrNull ->
            credentialsOrNull?.let { credentials ->
                InternalCredentials(
                    user = InputImpl(
                        scope = scope,
                        editingString = credentials.user,
                        placeholder = skeleton.cachedLoginInfo?.auth?.user.orEmpty(),
                        tryParse = { input ->
                            input
                                .trim()
                                .some()
                        }
                    ),
                    password = InputImpl(
                        scope = scope,
                        editingString = credentials.password,
                        placeholder = skeleton.cachedLoginInfo?.auth?.password.orEmpty(),
                        tryParse = { input ->
                            input
                                .trim()
                                .takeIf(String::isNotEmpty)
                                .toOption()
                        }
                    )
                )
            }
        }

    data class Credentials(
        val user: Input,
        val password: Input,
    )

    val credentials: StateFlow<Credentials?> = _credentials.mapState(scope) { internalCredentialsOrNull ->
        internalCredentialsOrNull?.let { internalCredentials ->
            Credentials(
                user = internalCredentials.user,
                password = internalCredentials.password,
            )
        }
    }

    val isCredentialsOpened: StateFlow<Boolean> =
        credentials.mapState(scope) { it != null }

    fun switchCredentialsIsOpened() {
        skeleton.credentials.update {
            it.foldNullable(
                ifNull = { Skeleton.Auth.empty() },
                ifNotNull = { null }
            )
        }
    }

    private val loginInfoOrNull: StateFlow<LoginInfo?> = buildLoginInfo(scope)

    private fun buildLoginInfo(
        scope: CoroutineScope,
    ): StateFlow<LoginInfo?> = _address
        .value
        .scopedInState(scope)
        .flatMapState(scope) { (stateScope, addressOrNone) ->
            addressOrNone.fold(
                ifEmpty = { null.toMutableStateFlowAsInitial() },
                ifSome = { address ->
                    buildLoginInfo(
                        scope = stateScope,
                        address = address,
                    )
                }
            )
        }

    private fun buildLoginInfo(
        scope: CoroutineScope,
        address: String,
    ): StateFlow<LoginInfo?> = _port
        .value
        .scopedInState(scope)
        .flatMapState(scope) { (stateScope, portOrNone) ->
            portOrNone.fold(
                ifEmpty = { null.toMutableStateFlowAsInitial() },
                ifSome = { port ->
                    buildLoginInfo(
                        scope = stateScope,
                        address = address,
                        port = port,
                    )
                }
            )
        }

    private fun buildLoginInfo(
        scope: CoroutineScope,
        address: String,
        port: Int,
    ): StateFlow<LoginInfo?> = _clientId
        .value
        .scopedInState(scope)
        .flatMapState(scope) { (stateScope, clientIdOrNone) ->
            clientIdOrNone.fold(
                ifEmpty = { null.toMutableStateFlowAsInitial() },
                ifSome = { clientId ->
                    buildLoginInfo(
                        scope = stateScope,
                        address = address,
                        port = port,
                        clientId = clientId,
                    )
                }
            )
        }

    private fun buildLoginInfo(
        scope: CoroutineScope,
        address: String,
        port: Int,
        clientId: String,
    ): StateFlow<LoginInfo?> = _credentials
        .scopedInState(scope)
        .flatMapState(scope) { (stateScope, credentialsOrNull) ->
            credentialsOrNull.foldNullable(
                ifNull = {
                    LoginInfo(
                        address = address,
                        port = port,
                        clientId = clientId,
                        auth = null,
                    ).toMutableStateFlowAsInitial()
                },
                ifNotNull = { credentials ->
                    buildLoginInfo(
                        scope = stateScope,
                        address = address,
                        port = port,
                        clientId = clientId,
                        credentials = credentials,
                    )
                }
            )
        }

    private fun buildLoginInfo(
        scope: CoroutineScope,
        address: String,
        port: Int,
        clientId: String,
        credentials: InternalCredentials,
    ): StateFlow<LoginInfo?> = credentials
        .user
        .value
        .scopedInState(scope)
        .flatMapState(scope) { (stateScope, userOrNone) ->
            userOrNone.fold(
                ifEmpty = { null.toMutableStateFlowAsInitial() },
                ifSome = { user ->
                    buildLoginInfo(
                        scope = stateScope,
                        address = address,
                        port = port,
                        clientId = clientId,
                        user = user,
                        password = credentials.password,
                    )
                }
            )
        }

    private fun buildLoginInfo(
        scope: CoroutineScope,
        address: String,
        port: Int,
        clientId: String,
        user: String,
        password: InputImpl<String>,
    ): StateFlow<LoginInfo?> = password
        .value
        .mapState(scope) { passwordOrNone ->
            passwordOrNone.getOrNull()?.let { password ->
                LoginInfo(
                    address = address,
                    port = port,
                    clientId = clientId,
                    auth = LoginInfo.Auth(
                        user = user,
                        password = password,
                    ),
                )
            }
        }

    val loginOrLogginingOrDisabled: StateFlow<StateFlow<(() -> Unit)?>?> = loginInfoOrNull
        .mapWithScope(scope) { stateScope, loginInfoOrNull ->
            loginInfoOrNull?.let { loginInfo ->
                actionOrNullIfExecuting(
                    scope = stateScope,
                ) {
                    dependencies
                        .doLogin
                        .doLogin(loginInfo)
                }
            }
        }

    val goBackHandler: GoBackHandler = NeverGoBackHandler
}