package com.abplus.meishiplus.pdf

import com.abplus.meishiplus.data.entities.CardEntity

data class CardPdfExportResult(
    val filePath: String,
)

expect suspend fun createCardPdf(cardEntity: CardEntity): CardPdfExportResult
