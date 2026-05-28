package com.abplus.meishiplus.pdf

import com.abplus.meishiplus.data.entities.CardEntity

data class CardPdfExportResult(
    val filePath: String,
)

expect suspend fun createCardPdf(cardEntity: CardEntity): CardPdfExportResult

expect suspend fun createPostcardCardPdf(cardEntity: CardEntity): CardPdfExportResult

expect suspend fun createA4CardPdf(
    cardEntity: CardEntity,
    topMarginMm: Float,
    bottomMarginMm: Float,
    leftMarginMm: Float,
    rightMarginMm: Float,
): CardPdfExportResult

expect fun deletePdfFileQuietly(filePath: String)
