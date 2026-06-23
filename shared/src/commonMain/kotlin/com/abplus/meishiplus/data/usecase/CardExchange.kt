package com.abplus.meishiplus.data.usecase

import com.abplus.meishiplus.data.repositories.CardRepository

suspend fun exchangeCard(
    cardRepository: CardRepository,
    currentUid: String,
    currentCardId: String,
    partnerCardId: String,
) {
    if (currentCardId == partnerCardId) return

    val currentCard = cardRepository.getCard(currentCardId)
    require(currentCard.ownerUid == currentUid) {
        "現在の名刺の所有者ではありません。"
    }
    val partnerCard = cardRepository.getCard(partnerCardId)

    val updatedCurrentCard = currentCard.copy(
        partnerIds = currentCard.partnerIds
            .plus(partnerCard.id)
            .distinct(),
    )

    cardRepository.updateCard(updatedCurrentCard)
}
