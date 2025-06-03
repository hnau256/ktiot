package hnau.ktiot.client.model.init

import hnau.common.kotlin.Loadable
import hnau.common.kotlin.LoadableStateFlow
import hnau.common.kotlin.Loading
import hnau.common.kotlin.Ready
import hnau.common.kotlin.coroutines.flatMapState
import hnau.common.kotlin.coroutines.mapState
import hnau.common.kotlin.coroutines.mapWithScope
import hnau.common.kotlin.coroutines.scopedInState
import hnau.common.kotlin.coroutines.toMutableStateFlowAsInitial
import hnau.common.kotlin.fold
import hnau.common.kotlin.getOrInit
import hnau.common.kotlin.mapper.toMapper
import hnau.common.kotlin.shrinkType
import hnau.common.kotlin.toAccessor
import hnau.common.model.goback.GoBackHandler
import hnau.common.model.goback.NeverGoBackHandler
import hnau.common.model.preferences.Preferences
import hnau.common.model.preferences.map
import hnau.common.model.preferences.withDefault
import hnau.ktiot.client.model.LoggedModel
import hnau.ktiot.client.model.LoginModel
import hnau.pipe.annotations.Pipe
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger {  }

class InitModel(
    scope: CoroutineScope,
    private val dependencies: Dependencies,
    private val skeleton: Skeleton,
) {

    @Pipe
    interface Dependencies {

        val preferencesFactory: Preferences.Factory

        fun login(
            doLogin: DoLogin,
        ): LoginModel.Dependencies

        fun logged(
            doLogout: DoLogout,
        ): LoggedModel.Dependencies

        companion object
    }

    @Serializable
    data class Skeleton(
        var state: InitStateModel.Skeleton? = null,
    )

    val state: StateFlow<Loadable<InitStateModel>> = LoadableStateFlow(
        scope = scope,
    ) {
        dependencies
            .preferencesFactory
            .createPreferences(scope)
    }
        .scopedInState(scope)
        .flatMapState(scope) { (preferencesScope, preferencesOrLoading) ->
            preferencesOrLoading.fold(
                ifLoading = { Loading.toMutableStateFlowAsInitial() },
                ifReady = { preferences ->
                    withPreferences(
                        scope = preferencesScope,
                        preferences = preferences,
                    ).mapState(preferencesScope, ::Ready)
                }
            )
        }

    private fun withPreferences(
        scope: CoroutineScope,
        preferences: Preferences,
    ): StateFlow<InitStateModel> {

        val loginStatePreference = preferences["login_state"]
            .map(
                scope = scope,
                mapper = Json.toMapper(LoginState.serializer()),
            )
            .withDefault(scope) { LoginState.Logouted() }

        return loginStatePreference
            .value
            .mapWithScope(scope) { stateScope, loginState ->
                when (loginState) {
                    is LoginState.Logouted -> InitStateModel.Login(
                        model = LoginModel(
                            scope = stateScope,
                            dependencies = dependencies.login(
                                doLogin = { loginInfo ->
                                    loginStatePreference.update(
                                        LoginState.Logged(
                                            loginInfo = loginInfo,
                                        )
                                    )
                                }
                            ),
                            skeleton = skeleton::state
                                .toAccessor()
                                .shrinkType<_, InitStateModel.Skeleton.Login>()
                                .getOrInit {
                                    InitStateModel.Skeleton.Login(
                                        LoginModel.Skeleton(
                                            cachedLoginInfo = loginState.cachedLoginInfo,
                                        )
                                    )
                                }
                                .skeleton
                        )
                    )

                    is LoginState.Logged -> InitStateModel.Logged(
                        model = LoggedModel(
                            scope = stateScope,
                            dependencies = dependencies.logged(
                                doLogout = {
                                    loginStatePreference.update(
                                        LoginState.Logouted(
                                            cachedLoginInfo = loginState.loginInfo,
                                        )
                                    )
                                }
                            ),
                            skeleton = skeleton::state
                                .toAccessor()
                                .shrinkType<_, InitStateModel.Skeleton.Logged>()
                                .getOrInit {
                                    InitStateModel.Skeleton.Logged(
                                        LoggedModel.Skeleton(
                                            loginInfo = loginState.loginInfo,
                                        )
                                    )
                                }
                                .skeleton
                        )
                    )
                }
            }
    }

    val goBackHandler: GoBackHandler = state
        .scopedInState(scope)
        .flatMapState(scope) { (stateScope, stateOrLoading) ->
            stateOrLoading.fold(
                ifLoading = { NeverGoBackHandler },
                ifReady = { state -> state.goBackHandler }
            )
        }
}