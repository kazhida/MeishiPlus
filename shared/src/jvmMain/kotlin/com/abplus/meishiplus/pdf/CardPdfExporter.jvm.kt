package com.abplus.meishiplus.pdf

import com.abplus.meishiplus.data.entities.CardEntity

actual suspend fun createCardPdf(cardEntity: CardEntity): CardPdfExportResult {
    throw UnsupportedOperationException("Desktop PDF export is not implemented.")
}
