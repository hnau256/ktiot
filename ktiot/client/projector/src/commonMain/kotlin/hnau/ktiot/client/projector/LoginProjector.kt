package hnau.ktiot.client.projector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import hnau.common.kotlin.coroutines.mapState
import hnau.common.kotlin.foldBoolean
import hnau.common.kotlin.ifTrue
import hnau.common.projector.uikit.TextInput
import hnau.common.projector.uikit.table.CellScope
import hnau.common.projector.uikit.table.Subtable
import hnau.common.projector.uikit.table.Table
import hnau.common.projector.uikit.table.TableOrientation
import hnau.common.projector.uikit.utils.Dimens
import hnau.common.projector.utils.horizontalDisplayPadding
import hnau.common.projector.utils.verticalDisplayPadding
import hnau.ktiot.client.model.LoginModel
import hnau.ktiot.client.projector.utils.Button
import hnau.pipe.annotations.Pipe
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource

class LoginProjector(
    scope: CoroutineScope,
    private val model: LoginModel,
    dependencies: Dependencies,
) {

    @Pipe
    interface Dependencies {

    }

    private val visibleItems: StateFlow<ImmutableList<Item>> = model.useCredentials.mapState(
        scope = scope,
    ) { useCredentials ->
        buildList {
            add(Item.AddressWithPort)
            add(
                Item.ClientId(
                    isLast = !useCredentials,
                )
            )
            add(Item.AuthSwitcher)
            useCredentials.ifTrue {
                add(Item.User)
                add(Item.Password)
            }
        }.toImmutableList()
    }

    @Composable
    fun Content() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .horizontalDisplayPadding()
                .verticalDisplayPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(
                space = Dimens.separation,
                alignment = Alignment.CenterVertically,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Table(
                modifier = Modifier.requiredWidthIn(
                    max = 320.dp,
                ),
                orientation = TableOrientation.Vertical,
                items = visibleItems.collectAsState().value,
            ) { item ->
                when (item) {
                    Item.AddressWithPort -> AddressWithPort()
                    is Item.ClientId -> ClientId(isLast = item.isLast)
                    Item.AuthSwitcher -> AuthSwitcher()
                    is Item.User -> User(model.user)
                    is Item.Password -> Password(model.password)
                }
            }
            model
                .loginOrLogginingOrDisabled
                .collectAsState()
                .value
                .Button { Text(stringResource(Res.string.login)) }
        }
    }

    @Composable
    private fun CellScope.AddressWithPort() {
        Subtable(
            items = remember { persistentListOf(false, true) }
        ) { item ->
            when (item) {
                false -> {
                    val focusRequester = remember { FocusRequester() }
                    Input(
                        label = stringResource(Res.string.address),
                        input = model.address,
                        shape = shape,
                        keyboardType = KeyboardType.Uri,
                        modifier = Modifier
                            .weight(3f)
                            .focusRequester(focusRequester),
                    )
                    LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
                }

                true -> Input(
                    label = stringResource(Res.string.port),
                    input = model.port,
                    shape = shape,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    @Composable
    private fun CellScope.ClientId(
        isLast: Boolean,
    ) {
        Input(
            label = stringResource(Res.string.client_id),
            input = model.clientId,
            shape = shape,
            keyboardType = KeyboardType.Ascii,
            isLast = isLast,
        )
    }

    @Composable
    private fun CellScope.AuthSwitcher() {
        val shape = shape
        Row(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = shape,
                )
                .padding(
                    horizontal = Dimens.separation,
                    vertical = Dimens.smallSeparation,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.credentials),
                style = MaterialTheme.typography.titleMedium,
            )
            Switch(
                checked = model.useCredentials.collectAsState().value,
                onCheckedChange = { model.useCredentials.value = it },
            )
        }
    }

    @Composable
    private fun CellScope.User(
        input: LoginModel.Input,
    ) {
        Input(
            label = stringResource(Res.string.user),
            input = input,
            shape = shape,
            keyboardType = KeyboardType.Email,
        )
    }

    @Composable
    private fun CellScope.Password(
        input: LoginModel.Input,
    ) {
        Input(
            label = stringResource(Res.string.password),
            input = input,
            shape = shape,
            keyboardType = KeyboardType.Password,
            isLast = true,
        )
    }

    @Composable
    private fun Input(
        label: String,
        input: LoginModel.Input,
        shape: Shape,
        keyboardType: KeyboardType,
        modifier: Modifier = Modifier,
        isLast: Boolean = false,
    ) {
        TextInput(
            label = { Text(label) },
            modifier = modifier,
            shape = shape,
            value = input.editingString,
            isError = input.correct.collectAsState().value.not(),
            keyboardActions = KeyboardActions(
                onDone = isLast.ifTrue {
                    {
                        TODO("try login")
                    }
                }
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = isLast.foldBoolean(
                    ifTrue = { ImeAction.Done },
                    ifFalse = { ImeAction.Next },
                ),
                keyboardType = keyboardType,
            )
        )
    }

    @Immutable
    private sealed interface Item {

        @Immutable
        data object AddressWithPort : Item

        @Immutable
        data class ClientId(
            val isLast: Boolean,
        ) : Item

        @Immutable
        data object AuthSwitcher : Item

        @Immutable
        data object User : Item

        @Immutable
        data object Password : Item
    }
}