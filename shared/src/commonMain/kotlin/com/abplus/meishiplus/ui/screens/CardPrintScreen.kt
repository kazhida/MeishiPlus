package com.abplus.meishiplus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.abplus.meishiplus.data.entities.CardEntity
import com.abplus.meishiplus.pdf.createA4CardPdf
import com.abplus.meishiplus.pdf.createPostcardCardPdf
import kotlinx.coroutines.launch
import meishiplus.shared.generated.resources.Res
import meishiplus.shared.generated.resources.ic_home
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardPrintScreen(
    cardEntity: CardEntity,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onPdfExportClick: (CardPrintSettings) -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedPageSize by remember { mutableStateOf(CardPrintPageSize.Postcard) }
    var topMargin by remember { mutableStateOf("11") }
    var bottomMargin by remember { mutableStateOf("11") }
    var leftMargin by remember { mutableStateOf("14") }
    var rightMargin by remember { mutableStateOf("14") }
    var pdfMessage by remember(cardEntity.id) { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF出力") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF00AFAF),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_home),
                            contentDescription = "戻る",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            Text(
                text = "用紙サイズ",
                style = MaterialTheme.typography.titleMedium,
            )
            CardPrintPageSizeOption(
                text = "はがきサイズ",
                selected = selectedPageSize == CardPrintPageSize.Postcard,
                testTag = "postcard-page-size-radio",
                onClick = { selectedPageSize = CardPrintPageSize.Postcard },
            )
            CardPrintPageSizeOption(
                text = "A4サイズ",
                selected = selectedPageSize == CardPrintPageSize.A4,
                testTag = "a4-page-size-radio",
                onClick = { selectedPageSize = CardPrintPageSize.A4 },
            )

            if (selectedPageSize == CardPrintPageSize.A4) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "余白",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MarginTextField(
                            label = "上",
                            value = topMargin,
                            onValueChange = { topMargin = it },
                            modifier = Modifier.weight(1f),
                        )
                        MarginTextField(
                            label = "下",
                            value = bottomMargin,
                            onValueChange = { bottomMargin = it },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MarginTextField(
                            label = "左",
                            value = leftMargin,
                            onValueChange = { leftMargin = it },
                            modifier = Modifier.weight(1f),
                        )
                        MarginTextField(
                            label = "右",
                            value = rightMargin,
                            onValueChange = { rightMargin = it },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    val settings = CardPrintSettings(
                        pageSize = selectedPageSize,
                        topMarginMm = topMargin.toFloatOrNull() ?: 0f,
                        bottomMarginMm = bottomMargin.toFloatOrNull() ?: 0f,
                        leftMarginMm = leftMargin.toFloatOrNull() ?: 0f,
                        rightMarginMm = rightMargin.toFloatOrNull() ?: 0f,
                    )
                    onPdfExportClick(settings)
                    coroutineScope.launch {
                        pdfMessage = runCatching {
                            when (settings.pageSize) {
                                CardPrintPageSize.Postcard -> createPostcardCardPdf(cardEntity).filePath
                                CardPrintPageSize.A4 -> createA4CardPdf(
                                    cardEntity = cardEntity,
                                    topMarginMm = settings.topMarginMm,
                                    bottomMarginMm = settings.bottomMarginMm,
                                    leftMarginMm = settings.leftMarginMm,
                                    rightMarginMm = settings.rightMarginMm,
                                ).filePath
                            }
                        }.fold(
                            onSuccess = { filePath -> "PDFを作成しました: $filePath" },
                            onFailure = { throwable -> "PDF作成に失敗しました: ${throwable.message}" },
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("PDF出力")
            }
            pdfMessage?.let { message ->
                Text(
                    text = message,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun CardPrintPageSizeOption(
    text: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            modifier = Modifier.testTag(testTag),
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun MarginTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text("$label mm") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next,
        ),
    )
}

data class CardPrintSettings(
    val pageSize: CardPrintPageSize,
    val topMarginMm: Float,
    val bottomMarginMm: Float,
    val leftMarginMm: Float,
    val rightMarginMm: Float,
)

enum class CardPrintPageSize {
    Postcard,
    A4,
}
