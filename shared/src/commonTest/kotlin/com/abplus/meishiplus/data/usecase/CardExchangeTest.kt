package com.abplus.meishiplus.data.usecase

import com.abplus.meishiplus.data.entities.CardEntity
import com.abplus.meishiplus.data.repositories.CardRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class CardExchangeTest {
    @Test
    fun exchangeCard_linksPartnerIdOnCurrentCard() = runTest {
        val cardRepository = InMemoryCardRepository()
        val currentCard = cardRepository.addCard(
            CardEntity(
                ownerUid = "user-1",
                caption = "current",
            ),
        )
        val partnerCard = cardRepository.addCard(
            CardEntity(
                ownerUid = "user-2",
                caption = "partner",
            ),
        )

        exchangeCard(
            cardRepository = cardRepository,
            currentUid = "user-1",
            currentCardId = currentCard.id,
            partnerCardId = partnerCard.id,
        )

        assertEquals(
            listOf(partnerCard.id),
            cardRepository.getCard(currentCard.id).partnerIds,
        )
        assertEquals(
            emptyList(),
            cardRepository.getCard(partnerCard.id).partnerIds,
        )
    }

    @Test
    fun exchangeCard_doesNotDuplicateExistingPartnerIds() = runTest {
        val cardRepository = InMemoryCardRepository()
        val currentCard = cardRepository.addCard(
            CardEntity(ownerUid = "user-1"),
        )
        val partnerCard = cardRepository.addCard(
            CardEntity(ownerUid = "user-2"),
        )
        exchangeCard(
            cardRepository = cardRepository,
            currentUid = "user-1",
            currentCardId = currentCard.id,
            partnerCardId = partnerCard.id,
        )
        exchangeCard(
            cardRepository = cardRepository,
            currentUid = "user-1",
            currentCardId = currentCard.id,
            partnerCardId = partnerCard.id,
        )

        assertEquals(
            listOf(partnerCard.id),
            cardRepository.getCard(currentCard.id).partnerIds,
        )
        assertEquals(
            emptyList(),
            cardRepository.getCard(partnerCard.id).partnerIds,
        )
    }

    @Test
    fun exchangeCard_requiresCurrentUserToOwnTheCard() = runTest {
        val cardRepository = InMemoryCardRepository()
        val currentCard = cardRepository.addCard(
            CardEntity(ownerUid = "user-2"),
        )
        val partnerCard = cardRepository.addCard(
            CardEntity(ownerUid = "user-3"),
        )

        val error = assertFailsWith<IllegalArgumentException> {
            exchangeCard(
                cardRepository = cardRepository,
                currentUid = "user-1",
                currentCardId = currentCard.id,
                partnerCardId = partnerCard.id,
            )
        }

        assertEquals("現在の名刺の所有者ではありません。", error.message)
    }
}

private class InMemoryCardRepository : CardRepository {
    private val cards = mutableMapOf<String, CardEntity>()
    private var nextId = 0

    override suspend fun addCard(card: CardEntity): CardEntity {
        val id = "card-${nextId++}"
        val cardWithId = card.withInitializedLayout().copy(id = id)
        cards[id] = cardWithId
        return cardWithId
    }

    override suspend fun getCard(id: String): CardEntity =
        cards[id]?.withInitializedLayout() ?: error("Card not found: $id")

    override suspend fun getCards(cardIds: List<String>): List<CardEntity> =
        cardIds.map { id -> getCard(id) }

    override suspend fun getCardsByOwnerUid(ownerUid: String): List<CardEntity> =
        cards.values.filter { card -> card.ownerUid == ownerUid }

    override suspend fun saveCard(card: CardEntity) {
        cards[card.id] = card.withInitializedLayout()
    }

    override suspend fun deleteCard(id: String) {
        cards.remove(id)
    }

    override suspend fun updateCard(card: CardEntity) {
        saveCard(card)
    }

    override suspend fun appendPartnerId(cardId: String, partnerCardId: String) {
        val current = getCard(cardId)
        cards[cardId] = current.copy(
            partnerIds = current.partnerIds
                .plus(partnerCardId)
                .distinct(),
        )
    }
}
