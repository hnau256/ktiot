@file:UseSerializers(
    MutableStateFlowSerializer::class,
    EditingString.Serializer::class,
)
@file:OptIn(ExperimentalUuidApi::class)

package hnau.ktiot.client.model

import arrow.core.Option
import arrow.core.toOption
import hnau.common.kotlin.coroutines.actionOrNullIfExecuting
import hnau.common.kotlin.coroutines.flatMapState
import hnau.common.kotlin.coroutines.flow.state.mapState
import hnau.common.kotlin.coroutines.flow.state.mapWithScope
import hnau.common.kotlin.coroutines.scopedInState
import hnau.common.kotlin.coroutines.flow.state.mutable.toMutableStateFlowAsInitial
import hnau.common.kotlin.foldBoolean
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
        val user: MutableStateFlow<EditingString>,
        val password: MutableStateFlow<EditingString>,
        val useCredentials: MutableStateFlow<Boolean>,
    ) {

        constructor(
            cachedLoginInfo: LoginInfo?,
        ) : this(
            cachedLoginInfo = cachedLoginInfo,

            address = cachedLoginInfo
                ?.address
                .ifNull { "127.0.0.1" }
                .toEditingString()
                .toMutableStateFlowAsInitial(),

            port = cachedLoginInfo
                ?.port
                .ifNull { MqttConfig.DefaultPort }
                .toString()
                .toEditingString()
                .toMutableStateFlowAsInitial(),

            clientId = cachedLoginInfo
                ?.clientId
                .ifNull { Uuid.random().toString() }
                .toEditingString()
                .toMutableStateFlowAsInitial(),

            user = cachedLoginInfo
                ?.auth
                ?.user
                .orEmpty()
                .toEditingString()
                .toMutableStateFlowAsInitial(),

            password = cachedLoginInfo
                ?.auth
                ?.password
                .orEmpty()
                .toEditingString()
                .toMutableStateFlowAsInitial(),

            useCredentials = cachedLoginInfo
                ?.auth
                .let { it != null }
                .toMutableStateFlowAsInitial(),
        )
    }

    interface Input {

        val editingString: MutableStateFlow<EditingString>

        val correct: StateFlow<Boolean>
    }

    private class InputImpl<T>(
        scope: CoroutineScope,
        override val editingString: MutableStateFlow<EditingString>,
        private val tryParse: (String) -> Option<T>,
    ) : Input {

        val value: StateFlow<Option<T>> = editingString.mapState(
            scope = scope,
        ) { input ->
            input
                .text
                .takeIf(String::isNotEmpty)
                .toOption()
                .flatMap(tryParse)
        }

        override val correct: StateFlow<Boolean> =
            value.mapState(scope) { it.isSome() }
    }

    private val _address: InputImpl<String> = InputImpl(
        scope = scope,
        editingString = skeleton.address,
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
        tryParse = { input ->
            input
                .trim()
                .takeIf(String::isNotEmpty)
                .toOption()
        }
    )

    val clientId: Input
        get() = _clientId


    private val _user: InputImpl<String> = InputImpl(
        scope = scope,
        editingString = skeleton.user,
        tryParse = { input ->
            input
                .trim()
                .takeIf(String::isNotEmpty)
                .toOption()
        }
    )

    val user: Input
        get() = _user


    private val _password: InputImpl<String> = InputImpl(
        scope = scope,
        editingString = skeleton.password,
        tryParse = { input ->
            input
                .trim()
                .takeIf(String::isNotEmpty)
                .toOption()
        }
    )

    val password: Input
        get() = _password

    val useCredentials: MutableStateFlow<Boolean>
        get() = skeleton.useCredentials

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
    ): StateFlow<LoginInfo?> = useCredentials
        .scopedInState(scope)
        .flatMapState(scope) { (stateScope, useCredentials) ->
            useCredentials.foldBoolean(
                ifFalse = {
                    LoginInfo(
                        address = address,
                        port = port,
                        clientId = clientId,
                        auth = null,
                    ).toMutableStateFlowAsInitial()
                },
                ifTrue = {
                    buildLoginInfoWithCredentials(
                        scope = stateScope,
                        address = address,
                        port = port,
                        clientId = clientId,
                    )
                }
            )
        }

    private fun buildLoginInfoWithCredentials(
        scope: CoroutineScope,
        address: String,
        port: Int,
        clientId: String,
    ): StateFlow<LoginInfo?> = _user
        .value
        .scopedInState(scope)
        .flatMapState(scope) { (stateScope, userOrNone) ->
            userOrNone.fold(
                ifEmpty = { null.toMutableStateFlowAsInitial() },
                ifSome = { user ->
                    buildLoginInfoWithCredentials(
                        scope = stateScope,
                        address = address,
                        port = port,
                        clientId = clientId,
                        user = user,
                    )
                }
            )
        }

    private fun buildLoginInfoWithCredentials(
        scope: CoroutineScope,
        address: String,
        port: Int,
        clientId: String,
        user: String,
    ): StateFlow<LoginInfo?> = _password
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