package com.abplus.meishiplus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
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
                value = cardEntity.name,
                onValueChange = { onCardChange(cardEntity.copy(name = it)) },
                label = "氏名",
            )
            EntryTextField(
                value = cardEntity.organization,
                onValueChange = { onCardChange(cardEntity.copy(organization = it)) },
                label = "会社・組織",
            )
            EntryTextField(
                value = cardEntity.title,
                onValueChange = { onCardChange(cardEntity.copy(title = it)) },
                label = "役職",
            )
        }

        EntrySection(title = "連絡先") {
            EntryTextField(
                value = cardEntity.email,
                onValueChange = { onCardChange(cardEntity.copy(email = it)) },
                label = "メールアドレス",
                keyboardType = KeyboardType.Email,
            )
            EntryTextField(
                value = cardEntity.phone,
                onValueChange = { onCardChange(cardEntity.copy(phone = it)) },
                label = "電話番号",
                keyboardType = KeyboardType.Phone,
            )
            EntryTextField(
                value = cardEntity.address,
                onValueChange = { onCardChange(cardEntity.copy(address = it)) },
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

private fun currentEpochMillis(): Long =
    kotlin.time.Clock.System.now().toEpochMilliseconds()
