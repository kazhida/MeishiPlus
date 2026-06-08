package com.abplus.meishiplus.pdf

import com.abplus.meishiplus.data.entities.CardEntity
import java.io.File

actual fun deletePdfFileQuietly(filePath: String) {
    runCatching {
        val file = File(filePath)
        if (file.exists() && !file.delete()) {
            System.err.println("Failed to delete PDF file: $filePath")
        }
    }.onFailure { throwable ->
        System.err.println("Failed to delete PDF file: $filePath: ${throwable.message}")
    }
}

actual suspend fun createCardPdf(cardEntity: CardEntity): CardPdfExportResult {
    throw UnsupportedOperationException("Desktop PDF export is not implemented.")
}

actual suspend fun createPostcardCardPdf(cardEntity: CardEntity): CardPdfExportResult {
    throw UnsupportedOperationException("Desktop PDF export is not implemented.")
}

actual suspend fun createA4CardPdf(
    cardEntity: CardEntity,
    topMarginMm: Float,
    bottomMarginMm: Float,
    leftMarginMm: Float,
    rightMarginMm: Float,
): CardPdfExportResult {
    throw UnsupportedOperationException("Desktop PDF export is not implemented.")
}
