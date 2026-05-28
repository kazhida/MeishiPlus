package com.abplus.meishiplus.pdf

import com.abplus.meishiplus.data.entities.CardEntity

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
