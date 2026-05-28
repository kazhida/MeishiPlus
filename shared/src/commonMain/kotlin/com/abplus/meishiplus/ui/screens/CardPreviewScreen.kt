package com.abplus.meishiplus.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.abplus.meishiplus.data.entities.CardEntity
import com.abplus.meishiplus.pdf.createCardPdf
import com.abplus.meishiplus.pdf.deletePdfFileQuietly
import com.abplus.meishiplus.ui.components.PdfPreview

@Composable
fun CardPreviewScreen(
    cardEntity: CardEntity,
    onBackClick: () -> Unit = {},
) {
    var pdfPath by remember(cardEntity.id) { mutableStateOf<String?>(null) }
    var errorMessage by remember(cardEntity.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(cardEntity) {
        pdfPath = null
        errorMessage = null
        runCatching {
            createCardPdf(cardEntity).filePath
        }.onSuccess { path ->
            pdfPath = path
        }.onFailure { throwable ->
            errorMessage = throwable.message ?: "PDFの作成に失敗しました"
        }
    }

    DisposableEffect(pdfPath) {
        onDispose {
            pdfPath?.let { path ->
                deletePdfFileQuietly(path)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            errorMessage != null -> Text(
                text = errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center),
            )
            pdfPath == null -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )
            else -> {
                PdfPreview(
                    filePath = pdfPath.orEmpty(),
                    modifier = Modifier.fillMaxSize(),
                    rotateClockwise = true,
                    fillBounds = false,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onBackClick),
                )
            }
        }
    }
}
