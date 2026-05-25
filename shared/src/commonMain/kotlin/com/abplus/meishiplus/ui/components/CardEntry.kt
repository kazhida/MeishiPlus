package com.abplus.meishiplus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.abplus.meishiplus.data.entities.CardEntity

@Composable
fun CardEntry(
    cardEntity: CardEntity,
    onCardChange: (CardEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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
                keyboardType = KeyboardType.Phone,
            )
            EntryTextField(
                value = cardEntity.address.value,
                onValueChange = { onCardChange(cardEntity.copy(address = cardEntity.address.copy(value = it))) },
                label = "住所",
                imeAction = ImeAction.Done,
                minLines = 2,
            )
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
    supportingText: String? = null,
) {
    val focusManager = LocalFocusManager.current
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
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    textFieldValue = textFieldValue.copy(
                        selection = TextRange(0, textFieldValue.text.length),
                    )
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
        supportingText = supportingText?.let { text ->
            { Text(text) }
        },
    )
}
