package com.abplus.meishiplus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abplus.meishiplus.data.entities.CardEntity
import com.abplus.meishiplus.data.model.Account
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import meishiplus.shared.generated.resources.Res
import meishiplus.shared.generated.resources.ic_sns_facebook
import meishiplus.shared.generated.resources.ic_sns_github
import meishiplus.shared.generated.resources.ic_sns_google
import meishiplus.shared.generated.resources.ic_sns_instagram
import meishiplus.shared.generated.resources.ic_sns_qiita
import meishiplus.shared.generated.resources.ic_sns_x
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun CardEntry(
    cardEntity: CardEntity,
    onCardChange: (CardEntity) -> Unit,
    modifier: Modifier = Modifier,
    authenticatedAccounts: List<Account> = emptyList(),
    onAuthenticatedAccountCheckedChange: (Account, Boolean) -> Unit = { _, _ -> },
) {
    val accountItems = authenticatedAccounts.map { it.toCardAccountItemSpec() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            EntrySection(title = "基本情報") {
                EntryTextField(
                    value = cardEntity.caption,
                    onValueChange = { onCardChange(cardEntity.copy(caption = it)) },
                    label = "タブ見出し",
                )
                EntryTextField(
                    value = cardEntity.name.value,
                    onValueChange = { onCardChange(cardEntity.copy(name = cardEntity.name.copy(value = it))) },
                    label = "氏名",
                )
                EntryTextField(
                    value = cardEntity.organization.value,
                    onValueChange = { onCardChange(cardEntity.copy(organization = cardEntity.organization.copy(value = it))) },
                    label = "会社・組織",
                )
                EntryTextField(
                    value = cardEntity.title.value,
                    onValueChange = { onCardChange(cardEntity.copy(title = cardEntity.title.copy(value = it))) },
                    label = "役職",
                )
            }
        }

        item {
            EntrySection(title = "連絡先") {
                EntryTextField(
                    value = cardEntity.email.value,
                    onValueChange = { onCardChange(cardEntity.copy(email = cardEntity.email.copy(value = it))) },
                    label = "メールアドレス",
                    keyboardType = KeyboardType.Email,
                )
                EntryTextField(
                    value = cardEntity.phone.value,
                    onValueChange =
                        {
                            onCardChange(
                                cardEntity.copy(
                                    phone = cardEntity.phone.copy(value = it)
                                )
                            )
                        },
                    label = "電話番号",
                )
                EntryTextField(
                    value = cardEntity.address1.value,
                    onValueChange = { onCardChange(cardEntity.copy(address1 = cardEntity.address1.copy(value = it))) },
                    label = "住所1",
                    imeAction = ImeAction.Next,
                )
                EntryTextField(
                    value = cardEntity.address2.value,
                    onValueChange = { onCardChange(cardEntity.copy(address2 = cardEntity.address2.copy(value = it))) },
                    label = "住所2",
                    imeAction = ImeAction.Next,
                )
            }
        }

        if (accountItems.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "認証済みSNS",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    accountItems.forEach { item ->
                        val isChecked = cardEntity.accounts.any {
                            it.service.equals(item.account.service, ignoreCase = true)
                        }
                        CardAccountListItem(
                            item = item,
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                onAuthenticatedAccountCheckedChange(item.account, checked)
                            },
                        )
                    }
                }
            }
        }

        item {
            EntrySection(
                title = "備考",
            ) {
                EntryTextField(
                    value = cardEntity.remark,
                    onValueChange = { onCardChange(cardEntity.copy(remark = it)) },
                    label = "自己紹介・PRなど",
                    minLines = 3,
                    maxLines = 8,
                    singleLine = false,
                    imeAction = ImeAction.Default,
                )
            }
        }
    }
}

@Composable
private fun EntrySection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

@Composable
private fun EntryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    minLines: Int = 1,
    maxLines: Int = 1,
    singleLine: Boolean = true,
    supportingText: String? = null,
) {
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(value))
    }

    LaunchedEffect(value) {
        if (value != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(
                text = value,
                selection = TextRange(value.length),
            )
        }
    }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            textFieldValue = newValue
            if (newValue.text != value) {
                onValueChange(newValue.text)
            }
        },
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    textFieldValue = textFieldValue.copy(
                        selection = TextRange(0, textFieldValue.text.length),
                    )
                    coroutineScope.launch {
                        delay(120)
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) },
            onDone = { focusManager.clearFocus() },
        ),
        minLines = minLines,
        maxLines = maxLines,
        singleLine = singleLine,
        supportingText = supportingText?.let { text ->
            { Text(text) }
        },
    )
}

@Composable
private fun CardAccountListItem(
    item: CardAccountItemSpec,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(item.icon),
            contentDescription = item.serviceName,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.serviceName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.account.displayLabel(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Immutable
private data class CardAccountItemSpec(
    val serviceName: String,
    val icon: DrawableResource,
    val account: Account,
)

private fun Account.toCardAccountItemSpec(): CardAccountItemSpec {
    return CardAccountItemSpec(
        serviceName = serviceNameLabel(),
        icon = serviceIcon(),
        account = this,
    )
}

private fun Account.serviceNameLabel(): String {
    return when (this) {
        is Account.Github -> "GitHub"
        is Account.X -> "X"
        is Account.Qiita -> "Qiita"
        is Account.Facebook -> "Facebook"
        is Account.Instagram -> "Instagram"
        is Account.Google -> "Google"
    }
}

private fun Account.serviceIcon(): DrawableResource {
    return when (this) {
        is Account.Github -> Res.drawable.ic_sns_github
        is Account.X -> Res.drawable.ic_sns_x
        is Account.Qiita -> Res.drawable.ic_sns_qiita
        is Account.Facebook -> Res.drawable.ic_sns_facebook
        is Account.Instagram -> Res.drawable.ic_sns_instagram
        is Account.Google -> Res.drawable.ic_sns_google
    }
}

private fun Account.displayLabel(): String {
    return when (this) {
        is Account.X -> displayName?.takeIf { it.isNotBlank() } ?: userName
        else -> userName
    }
}
