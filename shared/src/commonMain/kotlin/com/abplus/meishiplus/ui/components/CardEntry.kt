package com.abplus.meishiplus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
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
    minLines: Int = 1,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        minLines = minLines,
        supportingText = supportingText?.let { text ->
            { Text(text) }
        },
    )
}
